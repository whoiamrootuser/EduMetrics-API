package br.edu.iftm.edumetrics.domain.dto;

/**
 * Projecao usada pela consulta de agregacao do ranking
 * ({@code DesempenhoRepository.findMediasPorAluno}).
 *
 * <p>Tipos casados com o retorno padrao do JPA: {@code avg()} -> {@link Double},
 * {@code count()} -> {@link Long}. O {@code RankingService} converte estes
 * valores para o {@link RankingItemDTO} final.</p>
 */
public record MediaAlunoDTO(
        String nome,
        String matricula,
        Double mediaGeral,
        Long disciplinasConcluidas
) {
}
