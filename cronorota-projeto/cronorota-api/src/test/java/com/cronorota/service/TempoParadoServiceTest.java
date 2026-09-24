package com.cronorota.service;

import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.cronorota.service.Fixtures.BRASILIA;
import static com.cronorota.service.Fixtures.as;
import static org.assertj.core.api.Assertions.assertThat;

// Casos da seção 14 do documento (roteiros A, B e C) e dos fluxos de exceção do UC08.
class TempoParadoServiceTest {

    private final TempoParadoService service = new TempoParadoService();

    @Test
    void pontoDePartidaNaoAcumulaTempo_RN01() {
        Ponto partida = Ponto.builder().ordem(1).dataHoraChegada(as(8, 0)).dataHoraSaida(as(9, 0)).build();
        assertThat(service.calcularTempoParadoDoPonto(partida)).isZero();
    }

    @Test
    void tempoParadoESaidaMenosChegada_RN02() {
        Ponto ponto = Ponto.builder().ordem(2).dataHoraChegada(as(9, 10)).dataHoraSaida(as(9, 25)).build();
        assertThat(service.calcularTempoParadoDoPonto(ponto)).isEqualTo(15);
    }

    @Test
    void pontoEmAtendimentoFicaSemTempo_UC08E2() {
        Ponto ponto = Ponto.builder().ordem(2).dataHoraChegada(as(9, 10)).build();
        assertThat(service.calcularTempoParadoDoPonto(ponto)).isNull();
    }

    @Test
    void paradaQueAtravessaAMeiaNoite_UC08E1() {
        Ponto ponto = Ponto.builder().ordem(3)
                .dataHoraChegada(OffsetDateTime.of(2026, 9, 24, 23, 40, 0, 0, BRASILIA))
                .dataHoraSaida(OffsetDateTime.of(2026, 9, 25, 0, 20, 0, 0, BRASILIA))
                .build();
        assertThat(service.calcularTempoParadoDoPonto(ponto)).isEqualTo(40);
    }

    @Test
    void roteiroA_totalE75MinutosE15e6PorCentoDaJornada() {
        Roteiro roteiroA = roteiroComTempos(15, 10, 50);
        int total = service.calcularTempoTotalDoRoteiro(roteiroA);

        assertThat(total).isEqualTo(75);
        assertThat(service.calcularPercentualJornada(total, 480)).isEqualByComparingTo(new BigDecimal("15.63"));
    }

    @Test
    void roteirosBeC_conferemComASecao14() {
        assertThat(service.calcularTempoTotalDoRoteiro(roteiroComTempos(10, 5, 26))).isEqualTo(41);
        assertThat(service.calcularTempoTotalDoRoteiro(roteiroComTempos(5, 10, 30))).isEqualTo(45);
    }

    @Test
    void pontosEmAtendimentoNaoEntramNoTotal_RN03() {
        Roteiro roteiro = roteiroComTempos(15, 10);
        roteiro.getPontos().add(Ponto.builder().ordem(4).tempoParadoMinutos(null).build());
        assertThat(service.calcularTempoTotalDoRoteiro(roteiro)).isEqualTo(25);
    }

    // Ponto 1 = partida (tempo 0), seguido dos tempos informados.
    private Roteiro roteiroComTempos(int... minutos) {
        List<Ponto> pontos = new ArrayList<>();
        pontos.add(Ponto.builder().ordem(1).tempoParadoMinutos(0).build());
        for (int i = 0; i < minutos.length; i++) {
            pontos.add(Ponto.builder().ordem(i + 2).tempoParadoMinutos(minutos[i]).build());
        }
        return Roteiro.builder().pontos(pontos).build();
    }
}
