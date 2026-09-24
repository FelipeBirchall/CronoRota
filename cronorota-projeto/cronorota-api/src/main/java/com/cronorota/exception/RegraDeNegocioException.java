package com.cronorota.exception;

// Lançada quando uma regra de negócio (RN01, RN10, etc.) é violada.
// Vira HTTP 422 (Unprocessable Entity) no GlobalExceptionHandler - é
// diferente de um erro de validação de formato (que vira 400).
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
