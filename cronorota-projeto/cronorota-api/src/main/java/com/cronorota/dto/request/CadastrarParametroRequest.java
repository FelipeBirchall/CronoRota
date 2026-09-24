package com.cronorota.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CadastrarParametroRequest(
        @NotNull @DecimalMin("0.01") BigDecimal valorCombustivel,
        @NotNull Integer jornadaPadraoMinutos,
        @NotNull LocalDate dataInicioVigencia
) {
}
