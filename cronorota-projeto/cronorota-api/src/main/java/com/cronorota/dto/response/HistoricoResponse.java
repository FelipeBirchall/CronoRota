package com.cronorota.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * UC09 - histórico de pontos e tempos parados do período. Cada linha é uma
 * parada concluída (chegada e saída registradas), com endereço e horários -
 * é o critério de aceitação da seção 18: "cada linha exibe endereço,
 * chegada, saída e tempo parado, sem campo vazio". Por isso o ponto de
 * partida (RN01, não tem chegada nem tempo) e os pontos ainda em
 * atendimento (UC08-E2) ficam fora da lista.
 */
public record HistoricoResponse(
        LocalDate inicio,
        LocalDate fim,
        int quantidadeRoteiros,
        int quantidadePontos,
        int tempoTotalParadoMinutos,
        BigDecimal mediaPorRoteiroMinutos,
        BigDecimal mediaPorPontoMinutos,
        List<Linha> pontos
) {
    public record Linha(
            Long roteiroId,
            LocalDate data,
            Long motoristaId,
            String motorista,
            int ordem,
            String endereco,
            OffsetDateTime dataHoraChegada,
            OffsetDateTime dataHoraSaida,
            int tempoParadoMinutos
    ) {
    }
}
