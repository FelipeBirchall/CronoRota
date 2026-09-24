package com.cronorota.relatorio;

import com.cronorota.dto.response.HistoricoResponse;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioTest {

    private static final LocalDate DIA = LocalDate.of(2026, 9, 24);

    // Instantes gravados em UTC; o relatório tem que mostrar no horário de Brasília.
    private static OffsetDateTime utc(int hora, int minuto) {
        return OffsetDateTime.of(2026, 9, 24, hora, minuto, 0, 0, ZoneOffset.UTC);
    }

    private DadosRelatorio dados() {
        List<HistoricoResponse.Linha> linhas = List.of(
                new HistoricoResponse.Linha(1L, DIA, 10L, "Ana Lima", 2, "Rua Peru, 55, Centro - Belo Horizonte/MG",
                        utc(12, 0), utc(12, 15), 15),
                new HistoricoResponse.Linha(1L, DIA, 10L, "=HYPERLINK(\"x\")", 3, "Av. João César; bloco B",
                        utc(13, 0), utc(13, 50), 50));
        HistoricoResponse historico = new HistoricoResponse(DIA, DIA, 1, 2, 65,
                new BigDecimal("65.0"), new BigDecimal("32.5"), linhas);
        return new DadosRelatorio(historico, linhas, "Toda a equipe",
                ZonedDateTime.of(2026, 9, 24, 18, 30, 0, 0, Formatacao.FUSO));
    }

    @Test
    void csvAbreNoExcelEmPortugues() {
        byte[] bytes = RelatorioCsv.gerar(dados());

        // BOM UTF-8: sem ele o Excel estraga os acentos.
        assertThat(Arrays.copyOfRange(bytes, 0, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        // UC14 passo 4: cabeçalho com período, filtro e geração.
        assertThat(csv).contains("Período;24/09/2026 a 24/09/2026\r\n")
                .contains("Filtro;Toda a equipe\r\n")
                .contains("Gerado em;24/09/2026 18:30\r\n");
        // Linha com horário convertido de UTC para Brasília (12:00Z -> 09:00).
        assertThat(csv).contains("24/09/2026;Ana Lima;1;2;Rua Peru, 55, Centro - Belo Horizonte/MG;09:00;09:15;15\r\n");
        // Totalizadores com vírgula decimal.
        assertThat(csv).contains("Tempo total parado (min);65\r\n").contains("Média por ponto (min);32,5\r\n");
    }

    @Test
    void csvEscapaSeparadorEBloqueiaFormula() {
        String csv = new String(RelatorioCsv.gerar(dados()), StandardCharsets.UTF_8);

        // Nome começando com "=" vira texto (apóstrofo) e, por ter aspas, vai entre aspas.
        assertThat(csv).contains(";\"'=HYPERLINK(\"\"x\"\")\";");
        // Endereço com ";" vai entre aspas para não quebrar as colunas.
        assertThat(csv).contains(";\"Av. João César; bloco B\";");
    }

    @Test
    void pdfTemCabecalhoLinhasETotais() throws Exception {
        byte[] pdf = RelatorioPdf.gerar(dados());

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader leitor = new PdfReader(pdf);
        String texto = new PdfTextExtractor(leitor).getTextFromPage(1);
        assertThat(texto).contains("Histórico de tempos parados")
                .contains("24/09/2026 a 24/09/2026")
                .contains("Toda a equipe")
                .contains("Rua Peru, 55")
                .contains("09:00")
                .contains("1h 5min"); // tempo total parado: 65 min
    }
}
