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
        var e = ponto.getEndereco();
        String enderecoFormatado = e.getLogradouro() + ", " + e.getBairro() + " - " + e.getCidade() + "/" + e.getUf();
        return new PontoResponse(
                ponto.getId(),
                ponto.getOrdem(),
                enderecoFormatado,
                ponto.getDataHoraChegada(),
                ponto.getDataHoraSaida(),
                ponto.getTempoParadoMinutos()
        );
    }
}
