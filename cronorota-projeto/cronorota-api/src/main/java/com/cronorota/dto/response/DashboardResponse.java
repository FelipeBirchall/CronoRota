package com.cronorota.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * UC10 - tudo o que o dashboard mostra para o recorte escolhido, calculado a
 * partir do MESMO conjunto de roteiros (critério de aceitação da seção 18:
 * os três recortes geram os gráficos "com os mesmos dados de base").
 */
public record DashboardResponse(
        LocalDate inicio,
        LocalDate fim,
        Indicadores indicadores,
        List<TotalNoIntervalo> porDia,
        List<TotalNoIntervalo> porMes,
        List<TotalDoRoteiro> porRoteiro,
        List<TotalDoEndereco> rankingEnderecos,
        List<TotalDoMotorista> porMotorista
) {
    /**
     * Painel de indicadores (UC10 passo 8).
     *
     * @param percentualJornadaMedio média do percentual da jornada (RN04) dos roteiros do recorte
     * @param custoEstimadoTotal     soma do custo estimado (RN07); nulo quando nenhum roteiro tem custo (UC10-E3)
     * @param custoCompleto          false se algum roteiro ficou sem custo (sem distância ou sem parâmetro)
     */
    public record Indicadores(
            int quantidadeRoteiros,
            int tempoTotalParadoMinutos,
            BigDecimal percentualJornadaMedio,
            PontoCritico pontoMaisCritico,
            BigDecimal custoEstimadoTotal,
            boolean custoCompleto
    ) {
    }

    // A parada individual mais longa do recorte - nula se não houver nenhuma.
    public record PontoCritico(Long roteiroId, LocalDate data, String motorista, String endereco, int tempoParadoMinutos) {
    }

    // Uma barra dos gráficos por dia (chave "2026-09-24") e por mês (chave "2026-09").
    public record TotalNoIntervalo(String chave, int tempoParadoMinutos, int quantidadeRoteiros) {
    }

    // Uma barra do gráfico por período: um roteiro do intervalo selecionado.
    public record TotalDoRoteiro(Long roteiroId, LocalDate data, String motorista, int tempoParadoMinutos,
                                 BigDecimal percentualJornada) {
    }

    // UC10-A2 - ranking dos endereços com maior tempo parado acumulado.
    public record TotalDoEndereco(Long enderecoId, String endereco, int tempoParadoMinutos, int paradas) {
    }

    // UC10-A3 - comparação do tempo parado médio entre motoristas da equipe.
    public record TotalDoMotorista(Long motoristaId, String motorista, int quantidadeRoteiros,
                                   int tempoParadoMinutos, BigDecimal mediaPorRoteiroMinutos) {
    }
}
