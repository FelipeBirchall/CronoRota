package com.cronorota.service;

import com.cronorota.exception.AcessoNegadoException;
import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Endereco;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.model.Pedido;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.model.SituacaoPedido;
import com.cronorota.repository.GerenteRepository;
import com.cronorota.repository.MotoristaRepository;
import com.cronorota.repository.PedidoRepository;
import com.cronorota.repository.RoteiroRepository;
import com.cronorota.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementa o UC06 (Montar roteiro diário). Depende do UC04 (pontos e
 * endereços já cadastrados) e do UC05 (pedidos já registrados) - aqui a
 * gente assume que os pedidos já existem e só orquestra a montagem.
 */
@Service
@RequiredArgsConstructor
public class RoteiroService {

    private final RoteiroRepository roteiroRepository;
    private final MotoristaRepository motoristaRepository;
    private final GerenteRepository gerenteRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * @param gerenteId o gerente LOGADO (vem do token, nunca do corpo da
     *                  requisição) - é ele quem fica registrado como
     *                  planejador do roteiro (seção 12.2).
     * @param pedidoIds lista de pedidos, JÁ NA ORDEM em que o gerente
     *                  organizou o trajeto (RN06) - a ordem da lista vira a
     *                  ordem do ponto. O primeiro item vira o ponto de
     *                  partida (RN01).
     */
    @Transactional
    public Roteiro montar(Long gerenteId, Long motoristaId, LocalDate data, List<Long> pedidoIds,
                           BigDecimal distanciaTotalKm) {

        if (pedidoIds == null || new HashSet<>(pedidoIds).size() != pedidoIds.size()) {
            throw new RegraDeNegocioException("A lista de pedidos não pode ter itens repetidos");
        }

        Gerente gerente = gerenteRepository.findById(gerenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gerente não encontrado: " + gerenteId));
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: " + motoristaId));

        // RN13: o gerente monta roteiros só para a própria equipe.
        if (!Objects.equals(motorista.getGerente().getId(), gerente.getId())) {
            throw new AcessoNegadoException("O motorista não pertence à sua equipe");
        }
        // UC02-A2 / pré-condição do UC06: motorista inativo não recebe roteiro novo.
        if (!motorista.isAtivo()) {
            throw new RegraDeNegocioException("Motorista inativo não pode receber roteiros");
        }
        // RN05 / UC06-E1: um roteiro por motorista por data.
        if (roteiroRepository.existsByMotorista_IdAndDataAndAtivoTrue(motoristaId, data)) {
            throw new RegraDeNegocioException("Este motorista já possui um roteiro em " + data);
        }

        Roteiro roteiro = Roteiro.builder()
                .motorista(motorista)
                .gerentePlanejador(gerente)
                .data(data)
                .distanciaTotalKm(distanciaTotalKm)
                .pontos(new ArrayList<>())
                .ativo(true)
                .build();

        // "Ponto atende Pedido (1 : 0..*)" - dois pedidos no mesmo endereço
        // viram UM ponto só, na posição do primeiro deles. Senão o motorista
        // teria que registrar chegada/saída duas vezes no mesmo lugar e o
        // tempo parado daquele endereço sairia partido em dois.
        Map<Long, Ponto> pontoPorEndereco = new LinkedHashMap<>();
        int ordem = 1;
        for (Long pedidoId : pedidoIds) {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + pedidoId));
            validarPedidoParaRoteiro(pedido, data);

            Endereco endereco = pedido.getEnderecoEntrega();
            Ponto ponto = pontoPorEndereco.get(endereco.getId());
            if (ponto == null) {
                ponto = Ponto.builder()
                        .roteiro(roteiro)
                        .ordem(ordem++)
                        .endereco(endereco)
                        .build();
                pontoPorEndereco.put(endereco.getId(), ponto);
                roteiro.getPontos().add(ponto);
            }

            pedido.setSituacao(SituacaoPedido.EM_ROTEIRO);
            pedido.setPonto(ponto);
        }

        // RN01/RN03: precisa de pelo menos 2 pontos (partida + um que acumula
        // tempo) - multiplicidade "1 : 2..*" do diagrama de classes (seção
        // 12.2). A checagem é sobre PONTOS, não pedidos: dois pedidos no
        // mesmo endereço formam um ponto só.
        if (roteiro.getPontos().size() < 2) {
            throw new RegraDeNegocioException("Um roteiro precisa de pelo menos 2 pontos em endereços diferentes (partida + destino)");
        }

        // O save do Roteiro salva também os Pontos em cascata (CascadeType.ALL
        // na entidade Roteiro), e os Pedidos são salvos porque a transação
        // gerenciada pelo @Transactional detecta a mudança no situacao/ponto
        // (dirty checking do JPA) e sincroniza ao final do método.
        return roteiroRepository.save(roteiro);
    }

    private void validarPedidoParaRoteiro(Pedido pedido, LocalDate dataRoteiro) {
        // RN12: pedidos cancelados não entram na montagem.
        if (pedido.getSituacao() == SituacaoPedido.CANCELADO) {
            throw new RegraDeNegocioException("Pedido " + pedido.getCodigo() + " está cancelado e não pode entrar no roteiro");
        }
        // Sem isto, um pedido que já está em outro roteiro seria "roubado"
        // por este, e o roteiro original ficaria com um ponto sem pedido.
        if (pedido.getSituacao() != SituacaoPedido.PENDENTE || pedido.getPonto() != null) {
            throw new RegraDeNegocioException("Pedido " + pedido.getCodigo() + " já está em outro roteiro");
        }
        // RN12: data prevista fora do período do roteiro (que é um dia só - RN05).
        if (!pedido.getDataPrevista().equals(dataRoteiro)) {
            throw new RegraDeNegocioException("Pedido " + pedido.getCodigo() + " está previsto para "
                    + pedido.getDataPrevista() + ", fora da data do roteiro");
        }
    }

    // RN13: o motorista consulta apenas os seus próprios roteiros - o id
    // vem do token (UsuarioAutenticado), nunca de um parâmetro que o
    // cliente poderia manipular pra ver o roteiro de outro motorista.
    public List<Roteiro> listarDoMotorista(Long motoristaId) {
        return roteiroRepository.findByMotorista_IdOrderByDataDesc(motoristaId);
    }

    /**
     * Roteiros ativos do período que o usuário pode ver (RN13), usados pelo
     * histórico (UC09) e pelo dashboard (UC10). O filtro de motorista é
     * opcional; se vier, precisa ser um motorista que o usuário já veria.
     */
    public List<Roteiro> buscarNoPeriodo(UsuarioAutenticado usuario, LocalDate inicio, LocalDate fim, Long motoristaId) {
        // UC09-E1: data inicial posterior à final.
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("Período inválido: a data inicial deve ser anterior ou igual à final");
        }

        Long gerenteId = null;
        if (usuario.isMotorista()) {
            if (motoristaId != null && !motoristaId.equals(usuario.id())) {
                throw new AcessoNegadoException("Você só pode consultar os seus próprios roteiros");
            }
            motoristaId = usuario.id();
        } else if (usuario.isGerente()) {
            gerenteId = usuario.id();
        }

        return roteiroRepository.buscarNoPeriodo(inicio, fim, gerenteId, motoristaId);
    }

    public Roteiro buscarPorId(Long id, UsuarioAutenticado usuario) {
        Roteiro roteiro = roteiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Roteiro não encontrado: " + id));
        verificarAcesso(roteiro, usuario);
        return roteiro;
    }

    /**
     * RN13 aplicada no servidor: o motorista acessa só os seus roteiros; o
     * gerente, os da sua equipe; o administrador, todos. O bloqueio de tela
     * no front-end é só conveniência - a seção 28.2 do documento exige que
     * a autorização seja verificada aqui, a cada requisição.
     */
    public void verificarAcesso(Roteiro roteiro, UsuarioAutenticado usuario) {
        if (usuario.isAdministrador()) {
            return;
        }
        Motorista motorista = roteiro.getMotorista();
        boolean permitido = (usuario.isMotorista() && Objects.equals(motorista.getId(), usuario.id()))
                || (usuario.isGerente() && Objects.equals(motorista.getGerente().getId(), usuario.id()));
        if (!permitido) {
            throw new AcessoNegadoException("Você não tem acesso a este roteiro");
        }
    }
}
