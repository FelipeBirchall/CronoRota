package com.cronorota.exception;

// Lançada quando o usuário está autenticado, mas o recurso pedido não é dele
// (RN13 - ex.: motorista abrindo o roteiro de outro motorista, gerente
// mexendo em motorista de outra equipe). Vira HTTP 403 no GlobalExceptionHandler.
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
