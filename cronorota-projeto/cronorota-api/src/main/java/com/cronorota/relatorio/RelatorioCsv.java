package com.cronorota.relatorio;

import com.cronorota.dto.response.HistoricoResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Relatório do período em CSV, no formato que o Excel em português abre
 * direto com duplo clique: separador ";" (a vírgula é o separador decimal
 * no Brasil), vírgula decimal e UTF-8 com BOM (sem o BOM, o Excel lê o
 * arquivo como Latin-1 e estraga os acentos dos endereços).
 */
public final class RelatorioCsv {

    private static final String SEPARADOR = ";";
    private static final String QUEBRA = "\r\n";
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private RelatorioCsv() {
    }

    public static byte[] gerar(DadosRelatorio dados) {
        HistoricoResponse h = dados.historico();
        StringBuilder csv = new StringBuilder();

        // UC14 passo 4: cabeçalho com período, filtro e data/hora de geração.
        linha(csv, "Relatório", "CronoRota - Histórico de tempos parados");
        linha(csv, "Período", Formatacao.periodo(h.inicio(), h.fim()));
        linha(csv, "Filtro", dados.filtro());
        linha(csv, "Gerado em", Formatacao.dataHora(dados.geradoEm()));
        csv.append(QUEBRA);

        // UC14 passo 3: pontos, endereços, horários e tempos parados. O tempo
        // vai em minutos (número) para quem quiser somar ou filtrar na planilha.
        linha(csv, "Data", "Motorista", "Roteiro", "Ponto", "Endereço", "Chegada", "Saída", "Tempo parado (min)");
        for (HistoricoResponse.Linha l : dados.linhas()) {
            linha(csv,
                    Formatacao.data(l.data()),
                    l.motorista(),
                    String.valueOf(l.roteiroId()),
                    String.valueOf(l.ordem()),
                    l.endereco(),
                    Formatacao.hora(l.dataHoraChegada()),
                    Formatacao.hora(l.dataHoraSaida()),
                    String.valueOf(l.tempoParadoMinutos()));
        }
        csv.append(QUEBRA);

        // ...e os totalizadores do período.
        linha(csv, "Roteiros", String.valueOf(h.quantidadeRoteiros()));
        linha(csv, "Paradas", String.valueOf(h.quantidadePontos()));
        linha(csv, "Tempo total parado (min)", String.valueOf(h.tempoTotalParadoMinutos()));
        linha(csv, "Média por roteiro (min)", Formatacao.decimal(h.mediaPorRoteiroMinutos()));
        linha(csv, "Média por ponto (min)", Formatacao.decimal(h.mediaPorPontoMinutos()));

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.writeBytes(BOM);
        saida.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return saida.toByteArray();
    }

    private static void linha(StringBuilder csv, String... campos) {
        csv.append(String.join(SEPARADOR, List.of(campos).stream().map(RelatorioCsv::campo).toList()));
        csv.append(QUEBRA);
    }

    /**
     * Escapa um campo. Dois cuidados:
     * - aspas em volta quando há separador, aspas ou quebra de linha no texto;
     * - "injeção de fórmula": nome e endereço são digitados por usuários, e um
     *   texto que comece com = + - @ seria executado como fórmula ao abrir a
     *   planilha. O apóstrofo na frente faz o Excel tratá-lo como texto.
     */
    static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(SEPARADOR) || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
