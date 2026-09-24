package com.cronorota.auditoria;

import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Administrador;
import com.cronorota.model.Endereco;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.model.Parametro;
import com.cronorota.model.Pedido;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.model.Usuario;
import com.cronorota.model.Veiculo;
import com.cronorota.relatorio.Formatacao;
import org.hibernate.proxy.HibernateProxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Os descritores de todas as entidades auditadas (as marcadas com @Audited
 * no pacote model). Relacionamentos aparecem como "#id": a versão histórica
 * do registro relacionado pode nem existir na trilha (ex.: um veículo
 * cadastrado antes da auditoria), e carregar só o id não depende dela.
 */
public final class DescritoresAuditoria {

    public static final DescritorAuditoria<Ponto> PONTO = new DescritorAuditoria<>(
            "PONTO", "Ponto", Ponto.class, Ponto::getId, p -> campos(
                    "Roteiro", ref(p.getRoteiro(), Roteiro::getId),
                    "Ordem", texto(p.getOrdem()),
                    "Endereço", ref(p.getEndereco(), Endereco::getId),
                    "Chegada", texto(p.getDataHoraChegada()),
                    "Saída", texto(p.getDataHoraSaida()),
                    "Tempo parado (min)", texto(p.getTempoParadoMinutos())));

    public static final DescritorAuditoria<Roteiro> ROTEIRO = new DescritorAuditoria<>(
            "ROTEIRO", "Roteiro", Roteiro.class, Roteiro::getId, r -> campos(
                    "Data", texto(r.getData()),
                    "Motorista", ref(r.getMotorista(), Motorista::getId),
                    "Gerente planejador", ref(r.getGerentePlanejador(), Gerente::getId),
                    "Distância (km)", texto(r.getDistanciaTotalKm()),
                    "Tempo total parado (min)", texto(r.getTempoTotalParadoMinutos()),
                    "% da jornada", texto(r.getPercentualJornada()),
                    "Custo estimado (R$)", texto(r.getCustoEstimado()),
                    "Ativo", texto(r.isAtivo())));

    public static final DescritorAuditoria<Parametro> PARAMETRO = new DescritorAuditoria<>(
            "PARAMETRO", "Parâmetro", Parametro.class, Parametro::getId, p -> campos(
                    "Valor do combustível (R$)", texto(p.getValorCombustivel()),
                    "Custo por km (R$)", texto(p.getCustoPorKm()),
                    "Jornada padrão (min)", texto(p.getJornadaPadraoMinutos()),
                    "Início da vigência", texto(p.getDataInicioVigencia()),
                    "Fim da vigência", texto(p.getDataFimVigencia())));

    public static final DescritorAuditoria<Pedido> PEDIDO = new DescritorAuditoria<>(
            "PEDIDO", "Pedido", Pedido.class, Pedido::getId, p -> campos(
                    "Código", p.getCodigo(),
                    "Destinatário", p.getDestinatario(),
                    "Endereço", ref(p.getEnderecoEntrega(), Endereco::getId),
                    "Data prevista", texto(p.getDataPrevista()),
                    "Janela de entrega", p.getJanelaEntrega(),
                    "Situação", p.getSituacao() == null ? null : p.getSituacao().name(),
                    "Ponto", ref(p.getPonto(), Ponto::getId)));

    public static final DescritorAuditoria<Motorista> MOTORISTA = new DescritorAuditoria<>(
            "MOTORISTA", "Motorista", Motorista.class, Motorista::getId, m -> {
                Map<String, String> campos = camposDeUsuario(m);
                campos.put("Documento", m.getDocumento());
                campos.put("Habilitação", m.getHabilitacao());
                campos.put("Veículo", ref(m.getVeiculo(), Veiculo::getId));
                campos.put("Gerente", ref(m.getGerente(), Gerente::getId));
                return campos;
            });

    public static final DescritorAuditoria<Gerente> GERENTE = new DescritorAuditoria<>(
            "GERENTE", "Gerente", Gerente.class, Gerente::getId, DescritoresAuditoria::camposDeUsuario);

    public static final DescritorAuditoria<Administrador> ADMINISTRADOR = new DescritorAuditoria<>(
            "ADMINISTRADOR", "Administrador", Administrador.class, Administrador::getId,
            DescritoresAuditoria::camposDeUsuario);

    public static final DescritorAuditoria<Veiculo> VEICULO = new DescritorAuditoria<>(
            "VEICULO", "Veículo", Veiculo.class, Veiculo::getId, v -> campos(
                    "Placa", v.getPlaca(),
                    "Modelo", v.getModelo(),
                    "Tipo", v.getTipo(),
                    "Rendimento (km/l)", texto(v.getRendimentoKmLitro())));

    public static final DescritorAuditoria<Endereco> ENDERECO = new DescritorAuditoria<>(
            "ENDERECO", "Endereço", Endereco.class, Endereco::getId, e -> campos(
                    "Logradouro", e.getLogradouro(),
                    "Bairro", e.getBairro(),
                    "Cidade", e.getCidade(),
                    "UF", e.getUf(),
                    "CEP", e.getCep(),
                    "Latitude", texto(e.getLatitude()),
                    "Longitude", texto(e.getLongitude())));

    public static final List<DescritorAuditoria<?>> TODOS = List.of(
            PONTO, ROTEIRO, PARAMETRO, PEDIDO, MOTORISTA, GERENTE, ADMINISTRADOR, VEICULO, ENDERECO);

    private DescritoresAuditoria() {
    }

    public static DescritorAuditoria<?> porChave(String chave) {
        return TODOS.stream()
                .filter(d -> d.chave().equalsIgnoreCase(chave))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Entidade de auditoria desconhecida: " + chave));
    }

    // O hash da senha nem é auditado (@NotAudited em Usuario).
    private static Map<String, String> camposDeUsuario(Usuario u) {
        return campos(
                "Nome", u.getNome(),
                "Telefone", u.getTelefone(),
                "E-mail", u.getEmail(),
                "Login", u.getLogin(),
                "Ativo", texto(u.isAtivo()));
    }

    private static Map<String, String> campos(String... rotuloValor) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (int i = 0; i < rotuloValor.length; i += 2) {
            campos.put(rotuloValor[i], rotuloValor[i + 1]);
        }
        return campos;
    }

    /**
     * "#id" de um relacionamento sem carregá-lo: nas versões históricas o
     * Envers devolve um proxy, e o id sai do próprio proxy.
     */
    private static <R> String ref(R relacionado, Function<R, Long> id) {
        if (relacionado == null) {
            return null;
        }
        if (relacionado instanceof HibernateProxy proxy) {
            return "#" + proxy.getHibernateLazyInitializer().getInternalIdentifier();
        }
        return "#" + id.apply(relacionado);
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof OffsetDateTime instante) {
            return Formatacao.dataHora(instante);
        }
        if (valor instanceof LocalDate data) {
            return Formatacao.data(data);
        }
        if (valor instanceof BigDecimal numero) {
            return Formatacao.decimal(numero);
        }
        if (valor instanceof Boolean sim) {
            return sim ? "sim" : "não";
        }
        return valor.toString();
    }
}
