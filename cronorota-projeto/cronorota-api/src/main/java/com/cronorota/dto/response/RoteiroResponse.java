package com.cronorota.dto.response;

import com.cronorota.model.Roteiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RoteiroResponse(
        Long id,
        LocalDate data,
        String motorista,
        Integer tempoTotalParadoMinutos,
        BigDecimal percentualJornada,
        BigDecimal custoEstimado,
        List<PontoResponse> pontos
) {
    public static RoteiroResponse fromEntity(Roteiro roteiro) {
        return new RoteiroResponse(
                roteiro.getId(),
                roteiro.getData(),
                roteiro.getMotorista().getNome(),
                roteiro.getTempoTotalParadoMinutos(),
                roteiro.getPercentualJornada(),
                roteiro.getCustoEstimado(),
                roteiro.getPontos().stream().map(PontoResponse::fromEntity).toList()
        );
    }
}
