package br.edu.iftm.edumetrics.exception;

/**
 * Lancada quando uma operacao viola uma regra de unicidade de negocio
 * (ex: matricula, email ou codigo de disciplina ja cadastrados).
 * Mapeada para HTTP 409 Conflict.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
