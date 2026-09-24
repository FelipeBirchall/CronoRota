package com.cronorota.auditoria;

import com.cronorota.dto.response.AlteracaoResponse;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Roteiro;
import com.cronorota.relatorio.Formatacao;
import com.cronorota.service.RoteiroService;
import com.cronorota.security.UsuarioAutenticado;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Leitura da trilha de auditoria gravada pelo Envers. A gravação em si não
 * passa por aqui: acontece sozinha a cada save de entidade @Audited, com o
 * autor carimbado pelo AutorRevisaoListener.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    // Teto de entradas por consulta - a trilha cresce sem parar, e a tela
    // é para investigar um período, não para despejar o histórico inteiro.
    public static final int LIMITE_PADRAO = 200;
    public static final int LIMITE_MAXIMO = 1000;

    private final EntityManager entityManager;
    private final RoteiroService roteiroService;

    /**
     * Trilha completa (administrador), mais recente primeiro.
     *
     * @param chaveEntidade nulo = todas as entidades auditadas
     */
    @Transactional(readOnly = true)
    public List<AlteracaoResponse> listar(LocalDate inicio, LocalDate fim, String chaveEntidade, Integer limite) {
        if (inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("Período inválido: a data inicial deve ser anterior ou igual à final");
        }
        int maximo = limite == null ? LIMITE_PADRAO : Math.clamp(limite, 1, LIMITE_MAXIMO);
        long inicioMs = inicio.atStartOfDay(Formatacao.FUSO).toInstant().toEpochMilli();
        long fimMs = fim.plusDays(1).atStartOfDay(Formatacao.FUSO).toInstant().toEpochMilli() - 1;
        AuditCriterion noPeriodo = AuditEntity.revisionProperty("instanteMs").between(inicioMs, fimMs);

        List<DescritorAuditoria<?>> descritores = chaveEntidade == null || chaveEntidade.isBlank()
                ? DescritoresAuditoria.TODOS
                : List.of(DescritoresAuditoria.porChave(chaveEntidade));

        List<AlteracaoResponse> resultado = new ArrayList<>();
        for (DescritorAuditoria<?> descritor : descritores) {
            resultado.addAll(consultar(descritor, noPeriodo, maximo));
        }
        return ordenarELimitar(resultado, maximo);
    }

    /**
     * Alterações de um roteiro e dos seus pontos (RNF05: "alterações em
     * pontos e horários") - para a tela de detalhe do roteiro. Mesma regra
     * de acesso de ver o roteiro (RN13).
     */
    @Transactional(readOnly = true)
    public List<AlteracaoResponse> alteracoesDoRoteiro(Long roteiroId, UsuarioAutenticado usuario) {
        Roteiro roteiro = roteiroService.buscarPorId(roteiroId, usuario);

        List<AlteracaoResponse> resultado = new ArrayList<>();
        resultado.addAll(consultar(DescritoresAuditoria.ROTEIRO, AuditEntity.id().eq(roteiro.getId()), LIMITE_MAXIMO));
        resultado.addAll(consultar(DescritoresAuditoria.PONTO,
                AuditEntity.relatedId("roteiro").eq(roteiro.getId()), LIMITE_MAXIMO));
        return ordenarELimitar(resultado, LIMITE_MAXIMO);
    }

    private List<AlteracaoResponse> consultar(DescritorAuditoria<?> descritor, AuditCriterion filtro, int maximo) {
        AuditReader leitor = AuditReaderFactory.get(entityManager);
        AuditQuery consulta = leitor.createQuery()
                .forRevisionsOfEntity(descritor.classe(), false, true)
                .add(filtro)
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(maximo);

        List<AlteracaoResponse> alteracoes = new ArrayList<>();
        for (Object linha : consulta.getResultList()) {
            Object[] colunas = (Object[]) linha;
            Object entidade = colunas[0];
            Revisao revisao = (Revisao) colunas[1];
            RevisionType tipo = (RevisionType) colunas[2];
            Long registroId = descritor.idDe(entidade);

            Map<String, String> anterior = tipo == RevisionType.ADD
                    ? Map.of()
                    : descritor.camposDe(versaoAnterior(leitor, descritor, registroId, revisao.getId()));
            Map<String, String> novo = tipo == RevisionType.DEL ? Map.of() : descritor.camposDe(entidade);

            List<AlteracaoResponse.Campo> campos = diferencas(anterior, novo);
            // Revisão que não mexeu em nenhum campo exibido (ex.: o Envers
            // registra o roteiro como "alterado" quando só a lista de pontos
            // dele mudou) - os pontos aparecem na própria entrada deles.
            if (tipo == RevisionType.MOD && campos.isEmpty()) {
                continue;
            }
            alteracoes.add(new AlteracaoResponse(revisao.getId(), revisao.getInstante(), revisao.getLogin(),
                    revisao.getPerfil(), descritor.chave(), descritor.nome(), registroId, operacao(tipo),
                    revisao.getJustificativa(), campos));
        }
        return alteracoes;
    }

    private Object versaoAnterior(AuditReader leitor, DescritorAuditoria<?> descritor, Long id, Long revisao) {
        List<?> anteriores = leitor.createQuery()
                .forRevisionsOfEntity(descritor.classe(), true, true)
                .add(AuditEntity.id().eq(id))
                .add(AuditEntity.revisionNumber().lt(revisao))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1)
                .getResultList();
        return anteriores.isEmpty() ? null : anteriores.get(0);
    }

    /**
     * Campos cujo valor mudou entre duas versões, na ordem do descritor. Numa
     * inclusão (anterior vazio) entram os campos preenchidos; numa remoção
     * (novo vazio), os que tinham valor.
     */
    static List<AlteracaoResponse.Campo> diferencas(Map<String, String> anterior, Map<String, String> novo) {
        Set<String> rotulos = new LinkedHashSet<>(novo.keySet());
        rotulos.addAll(anterior.keySet());

        List<AlteracaoResponse.Campo> campos = new ArrayList<>();
        for (String rotulo : rotulos) {
            String antes = anterior.get(rotulo);
            String depois = novo.get(rotulo);
            if (!Objects.equals(antes, depois)) {
                campos.add(new AlteracaoResponse.Campo(rotulo, antes, depois));
            }
        }
        return campos;
    }

    private static String operacao(RevisionType tipo) {
        return switch (tipo) {
            case ADD -> "INCLUSAO";
            case MOD -> "ALTERACAO";
            case DEL -> "REMOCAO";
        };
    }

    // Mais recente primeiro; na mesma revisão, uma ordem estável por entidade e id.
    private static List<AlteracaoResponse> ordenarELimitar(List<AlteracaoResponse> lista, int maximo) {
        return lista.stream()
                .sorted(Comparator.comparing(AlteracaoResponse::revisao).reversed()
                        .thenComparing(AlteracaoResponse::entidade)
                        .thenComparing(AlteracaoResponse::registroId))
                .limit(maximo)
                .toList();
    }
}
