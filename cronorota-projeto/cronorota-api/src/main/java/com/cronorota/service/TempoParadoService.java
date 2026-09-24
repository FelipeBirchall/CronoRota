package com.cronorota.service;

import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Implementa o UC08 (Calcular tempo parado) e as regras RN01 a RN04.
 *
 * Esta classe é, de propósito, a mais "isolada" do projeto: não depende de
 * repository nenhum, só recebe objetos já carregados e devolve os valores
 * calculados. Isso a torna fácil de testar com JUnit puro, sem precisar de
 * banco (é o que a seção 25.1 do documento chama de testes com Testcontainers
 * pra integração - mas a unidade da regra em si não precisa nem disso).
 */
@Service
public class TempoParadoService {

    /**
     * RN01 + RN02: o ponto de partida (ordem == 1) nunca acumula tempo
     * parado, mesmo que tenha chegada e saída registradas. Os demais pontos
     * usam a diferença entre saída e chegada.
     *
     * Duration.between() trabalha em cima do instante completo (data + hora
     * + fuso, já que Ponto usa OffsetDateTime), então uma parada que atravessa
     * a meia-noite (UC08-E1) já funciona corretamente sem tratamento especial.
     */
    public Integer calcularTempoParadoDoPonto(Ponto ponto) {
        if (ponto.getOrdem() != null && ponto.getOrdem() == 1) {
            return 0; // RN01
        }

        // UC08-E2: chegada sem saída ainda - ponto "em atendimento",
        // não entra no cálculo até a saída ser registrada.
        if (ponto.getDataHoraChegada() == null || ponto.getDataHoraSaida() == null) {
            return null;
        }

        long minutos = Duration.between(ponto.getDataHoraChegada(), ponto.getDataHoraSaida()).toMinutes();
        return (int) minutos; // RN02
    }

    /**
     * RN03: soma os tempos parados de todos os pontos do roteiro, exceto o
     * de partida. Pontos ainda "em atendimento" (tempoParadoMinutos nulo)
     * são ignorados na soma - eles entram quando a saída for registrada e o
     * recálculo disparar de novo.
     */
    public int calcularTempoTotalDoRoteiro(Roteiro roteiro) {
        return roteiro.getPontos().stream()
                .filter(ponto -> ponto.getTempoParadoMinutos() != null)
                .mapToInt(Ponto::getTempoParadoMinutos)
                .sum();
    }

    /**
     * RN04: percentual do tempo total parado sobre a jornada padrão.
     * Ex.: 75 minutos parado sobre uma jornada de 480 minutos = 15,6%.
     */
    public BigDecimal calcularPercentualJornada(int tempoTotalMinutos, int jornadaPadraoMinutos) {
        if (jornadaPadraoMinutos <= 0) {
            throw new IllegalArgumentException("Jornada padrão deve ser maior que zero");
        }
        return BigDecimal.valueOf(tempoTotalMinutos)
                .divide(BigDecimal.valueOf(jornadaPadraoMinutos), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
