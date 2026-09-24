package com.cronorota.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CadastrarParametroRequest(
        @NotNull @DecimalMin("0.01") BigDecimal valorCombustivel,
        // UC12-E1: jornada entre 1 h e 24 h.
        @NotNull @Min(value = 60, message = "A jornada padrão deve ter no mínimo 1 hora")
        @Max(value = 1440, message = "A jornada padrão deve ter no máximo 24 horas")
        Integer jornadaPadraoMinutos,
        @NotNull LocalDate dataInicioVigencia
) {
}
