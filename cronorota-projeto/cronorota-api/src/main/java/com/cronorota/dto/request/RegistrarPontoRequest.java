package com.cronorota.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record RegistrarPontoRequest(
        @NotNull OffsetDateTime dataHora
) {
}
