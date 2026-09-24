package com.cronorota.dto.response;

import java.time.OffsetDateTime;

// Formato padrão de erro devolvido pelo GlobalExceptionHandler - todo erro
// da API tem essa mesma forma, o que facilita o front-end tratar qualquer
// falha de um jeito genérico.
public record ErroResponse(
        OffsetDateTime timestamp,
        int status,
        String mensagem
) {
    public static ErroResponse of(int status, String mensagem) {
        return new ErroResponse(OffsetDateTime.now(), status, mensagem);
    }
}
