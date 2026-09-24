package com.cronorota.exception;

// Lançada quando um id referenciado (motoristaId, roteiroId, etc.) não
// existe no banco. Vira HTTP 404 no GlobalExceptionHandler.
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
