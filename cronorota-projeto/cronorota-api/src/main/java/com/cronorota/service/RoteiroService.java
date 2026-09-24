package com.cronorota.service;

import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
     * @param pedidoIds lista de pedidos, JÁ NA ORDEM em que o gerente
     *                  organizou o trajeto (RN06) - a ordem da lista vira a
     *                  ordem do ponto. O primeiro item vira o ponto de
     *                  partida (RN01).
     */
    @Transactional
    public Roteiro montar(Long motoristaId, Long gerenteId, LocalDate data, List<Long> pedidoIds,
                           java.math.BigDecimal distanciaTotalKm) {

        // RN01/RN03: precisa de pelo menos 2 pontos (partida + um que acumula tempo) -
        // ver a multiplicidade "1 : 2..*" do diagrama de classes (seção 12.2).
        if (pedidoIds == null || pedidoIds.size() < 2) {
            throw new RegraDeNegocioException("Um roteiro precisa de pelo menos 2 pontos (partida + destino)");
        }

        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: " + motoristaId));
        Gerente gerente = gerenteRepository.findById(gerenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gerente não encontrado: " + gerenteId));

        Roteiro roteiro = Roteiro.builder()
                .motorista(motorista)
                .gerentePlanejador(gerente)
                .data(data)
                .distanciaTotalKm(distanciaTotalKm)
                .pontos(new ArrayList<>())
                .ativo(true)
                .build();

        int ordem = 1;
        for (Long pedidoId : pedidoIds) {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + pedidoId));

            // RN12: pedidos cancelados ou fora do período não entram na montagem.
            if (pedido.getSituacao() == SituacaoPedido.CANCELADO) {
                throw new RegraDeNegocioException("Pedido " + pedido.getCodigo() + " está cancelado e não pode entrar no roteiro");
            }

            Ponto ponto = Ponto.builder()
                    .roteiro(roteiro)
                    .ordem(ordem++)
                    .endereco(pedido.getEnderecoEntrega())
                    .build();

            roteiro.getPontos().add(ponto);

            pedido.setSituacao(SituacaoPedido.EM_ROTEIRO);
            pedido.setPonto(ponto);
        }

        // O save do Roteiro salva também os Pontos em cascata (CascadeType.ALL
        // na entidade Roteiro), e os Pedidos são salvos porque a transação
        // gerenciada pelo @Transactional detecta a mudança no situacao/ponto
        // (dirty checking do JPA) e sincroniza ao final do método.
        return roteiroRepository.save(roteiro);
    }

    // RN13: o motorista consulta apenas os seus próprios roteiros - o id
    // vem do token (UsuarioAutenticado), nunca de um parâmetro que o
    // cliente poderia manipular pra ver o roteiro de outro motorista.
    public List<Roteiro> listarDoMotorista(Long motoristaId) {
        return roteiroRepository.findByMotorista_IdOrderByDataDesc(motoristaId);
    }

    public Roteiro buscarPorId(Long id) {
        return roteiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Roteiro não encontrado: " + id));
    }
}
