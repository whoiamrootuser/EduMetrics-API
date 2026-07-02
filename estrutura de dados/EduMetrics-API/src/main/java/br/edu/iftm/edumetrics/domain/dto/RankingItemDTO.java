package br.edu.iftm.edumetrics.domain.dto;

/**
 * Item do ranking de alunos por media geral.
 *
 * <p>Implementa {@link Comparable} em ordem CRESCENTE de media -- exatamente o
 * que o min-heap ({@code PriorityQueue}) do {@code RankingService} precisa para
 * descartar o menor elemento e manter os Top-K maiores.</p>
 */
public record RankingItemDTO(
        int posicao,
        String nome,
        String matricula,
        double mediaGeral,
        int disciplinasConcluidas
) implements Comparable<RankingItemDTO> {

    @Override
    public int compareTo(RankingItemDTO outro) {
        // Ordem crescente de media -- necessario para o min-heap do PriorityQueue
        return Double.compare(this.mediaGeral, outro.mediaGeral);
    }
}
