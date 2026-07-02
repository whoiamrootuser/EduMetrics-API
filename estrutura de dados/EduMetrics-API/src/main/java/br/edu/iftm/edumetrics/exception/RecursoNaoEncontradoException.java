package br.edu.iftm.edumetrics.exception;

/**
 * Excecao base para recursos nao encontrados (mapeada para HTTP 404
 * pelo {@code GlobalExceptionHandler}).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
