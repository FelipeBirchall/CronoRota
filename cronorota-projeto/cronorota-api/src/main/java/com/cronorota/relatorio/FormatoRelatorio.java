package com.cronorota.relatorio;

import com.cronorota.exception.RegraDeNegocioException;

// UC14 passo 2: os dois formatos que o gerente pode escolher.
public enum FormatoRelatorio {
    CSV("text/csv; charset=UTF-8", "csv"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extensao;

    FormatoRelatorio(String contentType, String extensao) {
        this.contentType = contentType;
        this.extensao = extensao;
    }

    public String contentType() {
        return contentType;
    }

    public String extensao() {
        return extensao;
    }

    // Aceita "csv" ou "CSV" vindo da URL.
    public static FormatoRelatorio de(String valor) {
        for (FormatoRelatorio formato : values()) {
            if (formato.name().equalsIgnoreCase(valor)) {
                return formato;
            }
        }
        throw new RegraDeNegocioException("Formato de exportação inválido: " + valor + " (use CSV ou PDF)");
    }
}
