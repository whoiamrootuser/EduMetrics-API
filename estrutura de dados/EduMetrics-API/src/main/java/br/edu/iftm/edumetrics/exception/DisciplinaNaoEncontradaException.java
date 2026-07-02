package br.edu.iftm.edumetrics.exception;

/**
 * Lancada quando uma disciplina nao e encontrada por id ou codigo.
 */
public class DisciplinaNaoEncontradaException extends RecursoNaoEncontradoException {

    public DisciplinaNaoEncontradaException(Long id) {
        super("Disciplina nao encontrada: id=" + id);
    }

    public DisciplinaNaoEncontradaException(String codigo) {
        super("Disciplina nao encontrada: codigo=" + codigo);
    }
}
