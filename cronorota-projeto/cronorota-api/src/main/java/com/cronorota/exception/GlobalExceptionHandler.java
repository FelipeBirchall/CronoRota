package com.cronorota.exception;

import com.cronorota.dto.response.ErroResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza a tradução de exceção Java -> resposta HTTP. Sem isso, cada
 * controller teria que ter try/catch repetido pra cada tipo de erro - aqui
 * é escrito uma vez só e vale pra toda a API (@RestControllerAdvice aplica
 * a TODOS os @RestController do projeto).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResponse.of(403, ex.getMessage()));
    }

    // Rede de segurança para o que escapar das validações dos services
    // (ex.: duas requisições simultâneas gravando o mesmo login, ou o
    // índice único de RN05 no banco). Sem isto, o cliente receberia um 500
    // genérico com stack trace no log e nenhuma mensagem útil.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.of(409, "O registro conflita com dados já cadastrados"));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErroResponse.of(422, ex.getMessage()));
    }

    // Disparada automaticamente pelo Spring quando um @Valid falha (ex.:
    // campo @NotBlank vazio, @DecimalMin violado) - pega a primeira mensagem
    // de erro pra devolver algo legível, em vez do objeto de erro verboso
    // padrão do Spring.
    // Parâmetro de consulta ausente ou em formato errado (ex.: ?inicio=ontem
    // no histórico) - mesmo formato de erro do resto da API, em vez do
    // corpo padrão do Spring.
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> handleParametroDeConsulta(Exception ex) {
        String nome = ex instanceof MissingServletRequestParameterException m ? m.getParameterName()
                : ((MethodArgumentTypeMismatchException) ex).getName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.of(400, "Parâmetro ausente ou inválido: " + nome));
    }

    // JSON malformado ou com tipo errado no corpo (ex.: data em formato inválido).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.of(400, "Corpo da requisição inválido"));
    }

    /**
     * Qualquer outra falha (ex.: UC14-E2, erro ao gerar o PDF): registra o
     * stack trace no log e devolve uma mensagem genérica no formato padrão -
     * sem expor detalhe interno ao cliente. Exceções do próprio Spring que já
     * carregam o status certo (405, 404 de rota etc.) passam com esse status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleInesperado(Exception ex) {
        if (ex instanceof ErrorResponse erroSpring) {
            int status = erroSpring.getStatusCode().value();
            return ResponseEntity.status(status).body(ErroResponse.of(status, erroSpring.getBody().getTitle()));
        }
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.of(500, "Erro interno ao processar a requisição. Tente novamente."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse("Dados inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.of(400, mensagem));
    }
}
