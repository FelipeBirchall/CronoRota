package com.cronorota.service;

import com.cronorota.dto.response.HistoricoResponse;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Motorista;
import com.cronorota.relatorio.FormatoRelatorio;
import com.cronorota.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.cronorota.service.Fixtures.as;
import static com.cronorota.service.Fixtures.logado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportacaoServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 9, 24);
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-09-24T21:30:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Mock HistoricoService historicoService;
    @Mock MotoristaService motoristaService;

    private final UsuarioAutenticado gerente = logado("GERENTE", 1L);

    private ExportacaoService service() {
        return new ExportacaoService(historicoService, motoristaService, RELOGIO);
    }

    private void historicoCom(List<HistoricoResponse.Linha> linhas) {
        when(historicoService.consultar(gerente, INICIO, FIM, null)).thenReturn(new HistoricoResponse(
                INICIO, FIM, 1, linhas.size(), linhas.stream().mapToInt(HistoricoResponse.Linha::tempoParadoMinutos).sum(),
                BigDecimal.ZERO, BigDecimal.ZERO, linhas));
    }

    private HistoricoResponse.Linha linha(int ordem, String endereco, int minutos) {
        return new HistoricoResponse.Linha(1L, INICIO, 10L, "Ana", ordem, endereco, as(9, 0), as(9, 0).plusMinutes(minutos), minutos);
    }

    @Test
    void csvComNomeDoArquivoFiltroEHoraDeGeracao() {
        historicoCom(List.of(linha(2, "Rua A", 15)));

        var arquivo = service().exportar(gerente, INICIO, FIM, null, FormatoRelatorio.CSV, false);

        assertThat(arquivo.nomeArquivo()).isEqualTo("cronorota-historico-2026-09-01_2026-09-24.csv");
        assertThat(arquivo.contentType()).startsWith("text/csv");
        String csv = new String(arquivo.conteudo(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Filtro;Toda a equipe").contains("Gerado em;24/09/2026 18:30");
    }

    @Test
    void mantemAOrdenacaoDaTela_UC09A3() {
        historicoCom(List.of(linha(2, "Rua A", 15), linha(3, "Rua B", 50), linha(4, "Rua C", 30)));

        String csv = new String(service().exportar(gerente, INICIO, FIM, null, FormatoRelatorio.CSV, true).conteudo(),
                StandardCharsets.UTF_8);

        assertThat(csv.indexOf("Rua B")).isLessThan(csv.indexOf("Rua C"));
        assertThat(csv.indexOf("Rua C")).isLessThan(csv.indexOf("Rua A"));
    }

    @Test
    void filtroPorMotoristaUsaONome() {
        when(historicoService.consultar(gerente, INICIO, FIM, 10L)).thenReturn(new HistoricoResponse(
                INICIO, FIM, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of()));
        when(motoristaService.buscarPorId(10L, gerente)).thenReturn(Motorista.builder().nome("Ana Lima").build());

        var arquivo = service().exportar(gerente, INICIO, FIM, 10L, FormatoRelatorio.PDF, false);

        assertThat(arquivo.nomeArquivo()).endsWith(".pdf");
        assertThat(arquivo.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void volumeAcimaDoLimiteEhRecusado_UC14E1() {
        List<HistoricoResponse.Linha> muitas = new ArrayList<>();
        IntStream.rangeClosed(0, ExportacaoService.LIMITE_LINHAS).forEach(i -> muitas.add(linha(2, "Rua", 1)));
        historicoCom(muitas);

        assertThatThrownBy(() -> service().exportar(gerente, INICIO, FIM, null, FormatoRelatorio.CSV, false))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Reduza o período");
    }

    @Test
    void formatoInvalidoEhRecusado() {
        assertThat(FormatoRelatorio.de("pdf")).isEqualTo(FormatoRelatorio.PDF);
        assertThatThrownBy(() -> FormatoRelatorio.de("xlsx")).isInstanceOf(RegraDeNegocioException.class);
    }
}
