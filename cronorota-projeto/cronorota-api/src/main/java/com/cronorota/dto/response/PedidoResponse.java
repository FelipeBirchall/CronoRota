package com.cronorota.dto.response;

import com.cronorota.model.Pedido;
import java.time.LocalDate;

public record PedidoResponse(Long id, String codigo, String destinatario, EnderecoResponse endereco,
                              LocalDate dataPrevista, String situacao) {
    public static PedidoResponse fromEntity(Pedido p) {
        return new PedidoResponse(
                p.getId(), p.getCodigo(), p.getDestinatario(),
                EnderecoResponse.fromEntity(p.getEnderecoEntrega()),
                p.getDataPrevista(), p.getSituacao().name()
        );
    }
}
