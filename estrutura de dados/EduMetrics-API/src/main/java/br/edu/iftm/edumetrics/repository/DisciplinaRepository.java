package br.edu.iftm.edumetrics.repository;

import br.edu.iftm.edumetrics.domain.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de Disciplina.
 */
@Repository
public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {

    Optional<Disciplina> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
