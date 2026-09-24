package com.cronorota.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MontarRoteiroRequest(
        @NotNull Long motoristaId,
        @NotNull Long gerenteId,
        @NotNull @FutureOrPresent LocalDate data,

        // Sem isso, o custo estimado (UC13/RN07) nunca teria como ser
        // calculado - é a distância total do trajeto, informada pelo
        // gerente na montagem (ex.: consultada no mapa da seção 16 do
        // documento, o esboço de tela "Montagem de roteiro").
        @NotNull @DecimalMin("0.1") BigDecimal distanciaTotalKm,

        // A ordem desta lista É a ordem dos pontos no roteiro (RN06) -
        // o primeiro pedido da lista vira o ponto de partida.
        @NotEmpty @Size(min = 2, message = "Um roteiro precisa de pelo menos 2 pontos")
        List<Long> pedidoIds
) {
}
