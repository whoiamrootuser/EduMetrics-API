package br.edu.iftm.edumetrics.exception;

/**
 * Lancada quando um aluno nao e encontrado por id ou por matricula.
 */
public class AlunoNaoEncontradoException extends RecursoNaoEncontradoException {

    public AlunoNaoEncontradoException(Long id) {
        super("Aluno nao encontrado: id=" + id);
    }

    public AlunoNaoEncontradoException(String matricula) {
        super("Aluno nao encontrado: matricula=" + matricula);
    }
}
