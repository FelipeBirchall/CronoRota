package com.cronorota.service;

import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Endereco;
import com.cronorota.model.Pedido;
import com.cronorota.model.SituacaoPedido;
import com.cronorota.repository.EnderecoRepository;
import com.cronorota.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

// Implementa o UC05 (Registrar pedidos).
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EnderecoRepository enderecoRepository;

    public Pedido cadastrar(String codigo, String destinatario, Long enderecoId,
                             LocalDate dataPrevista, String janelaEntrega) {
        // UC05-E1: código de pedido duplicado.
        if (pedidoRepository.existsByCodigo(codigo)) {
            throw new RegraDeNegocioException("Já existe um pedido com o código " + codigo);
        }
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado: " + enderecoId));

        Pedido pedido = Pedido.builder()
                .codigo(codigo).destinatario(destinatario)
                .enderecoEntrega(endereco).dataPrevista(dataPrevista)
                .janelaEntrega(janelaEntrega)
                .situacao(SituacaoPedido.PENDENTE)
                .build();
        return pedidoRepository.save(pedido);
    }

    // Usado pela tela de montagem de roteiro (UC06): só pedidos pendentes
    // e ainda não associados a um ponto entram na lista de seleção.
    // Com data informada, lista só os pedidos daquela data (RN12) - é o que
    // a montagem de roteiro usa, já que o roteiro é de um dia só.
    public List<Pedido> listarPendentes(LocalDate data) {
        return data == null
                ? pedidoRepository.findBySituacaoAndPonto_IsNull(SituacaoPedido.PENDENTE)
                : pedidoRepository.findBySituacaoAndPonto_IsNullAndDataPrevista(SituacaoPedido.PENDENTE, data);
    }
}
