package com.cronorota.service;

import com.cronorota.dto.response.DashboardResponse;
import com.cronorota.dto.response.HistoricoResponse;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.cronorota.service.Fixtures.BRASILIA;
import static com.cronorota.service.Fixtures.endereco;
import static com.cronorota.service.Fixtures.gerente;
import static com.cronorota.service.Fixtures.logado;
import static com.cronorota.service.Fixtures.motorista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Histórico (UC09) e dashboard (UC10) sobre os roteiros A, B e C da seção 14
 * do documento: A = 15+10+50 (75 min), B = 10+5+26 (41 min), C = 5+10+30
 * (45 min), total 161 min. Os dois services partem da mesma lista de
 * roteiros, e os totais precisam bater entre si.
 */
@ExtendWith(MockitoExtension.class)
class AgregacaoTest {

    private static final LocalDate DIA_1 = LocalDate.of(2026, 9, 29);
    private static final LocalDate DIA_2 = LocalDate.of(2026, 9, 30);
    private static final LocalDate DIA_3 = LocalDate.of(2026, 10, 1);

    @Mock RoteiroService roteiroService;

    private final TempoParadoService tempoParadoService = new TempoParadoService();
    private final UsuarioAutenticado gerenteLogado = logado("GERENTE", 1L);
    private Motorista ana;
    private Motorista bruno;

    @BeforeEach
    void setUp() {
        Gerente gerente = gerente(1L);
        ana = motorista(10L, gerente);
        bruno = motorista(11L, gerente);

        Roteiro a = roteiro(1L, ana, DIA_1, 15, 10, 50);
        a.getPontos().get(3).setEndereco(endereco(99)); // Av. João César: a parada de 50 min
        a.setPercentualJornada(new BigDecimal("15.63"));
        a.setCustoEstimado(new BigDecimal("24.00"));
        Roteiro b = roteiro(2L, bruno, DIA_1, 10, 5, 26);
        b.setPercentualJornada(new BigDecimal("8.54"));
        Roteiro c = roteiro(3L, ana, DIA_3, 5, 10, 30);
        c.setPercentualJornada(new BigDecimal("9.38"));
        c.setCustoEstimado(new BigDecimal("12.00"));

        when(roteiroService.buscarNoPeriodo(eq(gerenteLogado), any(), any(), any())).thenReturn(List.of(a, b, c));
    }

    @Test
    void historicoListaSoAsParadasConcluidasComTotalizadores() {
        HistoricoResponse historico = new HistoricoService(roteiroService, tempoParadoService)
                .consultar(gerenteLogado, DIA_1, DIA_3, null);

        // 3 roteiros × 3 paradas; a partida (RN01) não entra.
        assertThat(historico.pontos()).hasSize(9).allSatisfy(linha -> {
            assertThat(linha.ordem()).isGreaterThan(1);
            assertThat(linha.dataHoraChegada()).isNotNull();
            assertThat(linha.dataHoraSaida()).isNotNull();
        });
        assertThat(historico.tempoTotalParadoMinutos()).isEqualTo(161);
        assertThat(historico.mediaPorRoteiroMinutos()).isEqualByComparingTo("53.7");
        assertThat(historico.mediaPorPontoMinutos()).isEqualByComparingTo("17.9");
    }

    @Test
    void dashboardAgregaOsMesmosRoteirosDoHistorico() {
        DashboardResponse dashboard = new DashboardService(roteiroService, tempoParadoService)
                .montar(gerenteLogado, DIA_1, DIA_3, null);

        var indicadores = dashboard.indicadores();
        assertThat(indicadores.tempoTotalParadoMinutos()).isEqualTo(161);
        assertThat(indicadores.quantidadeRoteiros()).isEqualTo(3);
        assertThat(indicadores.percentualJornadaMedio()).isEqualByComparingTo("11.18");
        assertThat(indicadores.pontoMaisCritico().tempoParadoMinutos()).isEqualTo(50);
        assertThat(indicadores.pontoMaisCritico().endereco()).startsWith("Rua 99");
        // B não tem custo: soma os outros dois e avisa que está incompleto (UC10-E3).
        assertThat(indicadores.custoEstimadoTotal()).isEqualByComparingTo("36.00");
        assertThat(indicadores.custoCompleto()).isFalse();
    }

    @Test
    void porDiaCobreTodoOIntervaloInclusiveDiasSemRoteiro() {
        DashboardResponse dashboard = new DashboardService(roteiroService, tempoParadoService)
                .montar(gerenteLogado, DIA_1, DIA_3, null);

        assertThat(dashboard.porDia()).extracting(DashboardResponse.TotalNoIntervalo::chave)
                .containsExactly("2026-09-29", "2026-09-30", "2026-10-01");
        assertThat(dashboard.porDia()).extracting(DashboardResponse.TotalNoIntervalo::tempoParadoMinutos)
                .containsExactly(116, 0, 45);
        assertThat(dashboard.porMes()).extracting(DashboardResponse.TotalNoIntervalo::tempoParadoMinutos)
                .containsExactly(116, 45);
        assertThat(dashboard.porRoteiro()).extracting(DashboardResponse.TotalDoRoteiro::tempoParadoMinutos)
                .containsExactly(75, 41, 45);
    }

    @Test
    void rankingEComparacaoEntreMotoristas() {
        DashboardResponse dashboard = new DashboardService(roteiroService, tempoParadoService)
                .montar(gerenteLogado, DIA_1, DIA_3, null);

        // Ranking é por tempo ACUMULADO (UC10-A2): o endereço 4 soma B (26) + C (30)
        // e passa à frente da maior parada isolada (50 min, o ponto mais crítico).
        assertThat(dashboard.rankingEnderecos()).extracting(DashboardResponse.TotalDoEndereco::tempoParadoMinutos)
                .startsWith(56, 50);
        assertThat(dashboard.rankingEnderecos().get(0).paradas()).isEqualTo(2);
        // Ana: (75 + 45) / 2 = 60; Bruno: 41 / 1 = 41 - ordenado da maior média para a menor.
        assertThat(dashboard.porMotorista()).extracting(DashboardResponse.TotalDoMotorista::motorista)
                .containsExactly(ana.getNome(), bruno.getNome());
        assertThat(dashboard.porMotorista().get(0).mediaPorRoteiroMinutos()).isEqualByComparingTo("60.0");
    }

    // Partida (ordem 1, só saída) + uma parada concluída por tempo informado.
    private Roteiro roteiro(long id, Motorista motorista, LocalDate data, int... minutos) {
        Roteiro roteiro = Roteiro.builder().id(id).data(data).motorista(motorista).ativo(true)
                .pontos(new ArrayList<>()).build();
        OffsetDateTime relogio = data.atTime(8, 0).atOffset(BRASILIA);
        roteiro.getPontos().add(Ponto.builder().id(id * 10).ordem(1).roteiro(roteiro).endereco(endereco(1))
                .dataHoraSaida(relogio).tempoParadoMinutos(0).build());
        for (int i = 0; i < minutos.length; i++) {
            OffsetDateTime chegada = relogio.plusHours(i + 1);
            roteiro.getPontos().add(Ponto.builder().id(id * 10 + i + 1).ordem(i + 2).roteiro(roteiro)
                    .endereco(endereco(i + 2)).dataHoraChegada(chegada)
                    .dataHoraSaida(chegada.plusMinutes(minutos[i])).tempoParadoMinutos(minutos[i]).build());
        }
        return roteiro;
    }
}
