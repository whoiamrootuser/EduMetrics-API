package br.edu.iftm.edumetrics.repository;

import br.edu.iftm.edumetrics.domain.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de Aluno. A busca por matricula usa o indice unico
 * {@code idx_aluno_matricula} (arvore B+, O(log n) no banco).
 */
@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    boolean existsByEmail(String email);
}
