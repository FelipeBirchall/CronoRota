package com.cronorota.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * UC07-A3 - ajuste manual de horário pelo gerente.
 *
 * Os horários vêm como data/hora LOCAL (o que o gerente digitou) e o
 * back-end os interpreta no fuso da operação (America/Sao_Paulo, seção
 * 24.2) - assim o front-end não precisa calcular offset. Nulo = manter o
 * horário atual.
 */
public record AjustarHorarioRequest(
        LocalDateTime dataHoraChegada,
        LocalDateTime dataHoraSaida,
        // "Exige justificativa": um mínimo de caracteres evita um "ok" ou "."
        // só para passar pela validação.
        @NotBlank(message = "Informe a justificativa do ajuste")
        @Size(min = 10, max = 500, message = "A justificativa deve ter entre 10 e 500 caracteres")
        String justificativa
) {
}
