package com.cronorota.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// Formatação em pt-BR comum ao CSV, ao PDF e à trilha de auditoria, para
// todos mostrarem exatamente os mesmos textos.
public final class Formatacao {

    // Horários sempre no fuso da operação (seção 24.2 do documento), não no
    // fuso do servidor - o instante é gravado com fuso (timestamptz) e só é
    // convertido aqui, na hora de exibir.
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatacao() {
    }

    public static String data(LocalDate data) {
        return DATA.format(data);
    }

    public static String hora(OffsetDateTime instante) {
        return instante == null ? "" : HORA.format(instante.atZoneSameInstant(FUSO));
    }

    public static String dataHora(ZonedDateTime instante) {
        return DATA_HORA.format(instante.withZoneSameInstant(FUSO));
    }

    public static String periodo(LocalDate inicio, LocalDate fim) {
        return data(inicio) + " a " + data(fim);
    }

    // 75 -> "1h 15min"; 45 -> "45min"
    public static String minutos(int minutos) {
        int h = minutos / 60;
        int m = minutos % 60;
        return h > 0 ? h + "h " + m + "min" : m + "min";
    }

    public static String dataHora(OffsetDateTime instante) {
        return instante == null ? "" : DATA_HORA.format(instante.atZoneSameInstant(FUSO));
    }

    // 53.7 -> "53,7" (vírgula decimal: é o que o Excel em português espera)
    public static String decimal(BigDecimal valor) {
        return valor.toPlainString().replace('.', ',');
    }
}
