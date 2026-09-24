package com.cronorota.dto.response;

import com.cronorota.model.Ponto;
import java.time.OffsetDateTime;

public record PontoResponse(
        Long id,
        Integer ordem,
        String endereco,
        OffsetDateTime dataHoraChegada,
        OffsetDateTime dataHoraSaida,
        Integer tempoParadoMinutos
) {
    public static PontoResponse fromEntity(Ponto ponto) {
        return new PontoResponse(
                ponto.getId(),
                ponto.getOrdem(),
                EnderecoResponse.formatado(ponto.getEndereco()),
                ponto.getDataHoraChegada(),
                ponto.getDataHoraSaida(),
                ponto.getTempoParadoMinutos()
        );
    }
}
