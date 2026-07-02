package br.edu.iftm.edumetrics.repository;

import br.edu.iftm.edumetrics.domain.Desempenho;
import br.edu.iftm.edumetrics.domain.dto.MediaAlunoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de Desempenho.
 */
@Repository
public interface DesempenhoRepository extends JpaRepository<Desempenho, Long> {

    /**
     * Agrega a media geral e o numero de disciplinas concluidas por aluno.
     * Alimenta o min-heap do {@code RankingService}.
     *
     * <p>{@code avg()} retorna Double e {@code count()} retorna Long (JPA),
     * casados com o construtor do {@link MediaAlunoDTO}.</p>
     */
    @Query("""
            select new br.edu.iftm.edumetrics.domain.dto.MediaAlunoDTO(
                       a.nome, a.matricula, avg(d.notaFinal), count(d.id))
            from Desempenho d
            join d.aluno a
            where d.notaFinal is not null
            group by a.id, a.nome, a.matricula
            """)
    List<MediaAlunoDTO> findMediasPorAluno();

    /**
     * Lista os desempenhos de um aluno ja trazendo a disciplina no mesmo SELECT
     * (JOIN FETCH) -- evita o problema N+1 (1 unica query em vez de 1 + N).
     */
    @Query("""
            select d
            from Desempenho d
            join fetch d.disciplina
            where d.aluno.id = :alunoId
            order by d.disciplina.nome
            """)
    List<Desempenho> findByAlunoIdFetchDisciplina(@Param("alunoId") Long alunoId);
}
