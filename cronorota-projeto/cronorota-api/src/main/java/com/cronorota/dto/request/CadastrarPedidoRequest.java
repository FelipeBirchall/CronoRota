package com.cronorota.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CadastrarPedidoRequest(
        @NotBlank String codigo,
        @NotBlank String destinatario,
        @NotNull Long enderecoId,
        @NotNull @FutureOrPresent LocalDate dataPrevista,
        String janelaEntrega
) {
}
