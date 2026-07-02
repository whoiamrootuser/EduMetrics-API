package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.dto.RankingItemDTO;
import br.edu.iftm.edumetrics.repository.DesempenhoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Servico de ranking (Top-K alunos por media geral) usando um min-heap.
 */
@Service
public class RankingService {

    private final DesempenhoRepository desempenhoRepo;

    public RankingService(DesempenhoRepository desempenhoRepo) {
        this.desempenhoRepo = desempenhoRepo;
    }

    /**
     * Retorna os top-K alunos por media geral.
     *
     * <p><b>Algoritmo:</b> min-heap de tamanho K. Para cada aluno, insere no heap;
     * se {@code heap.size() > K}, remove o menor (menor media). Ao final, o heap
     * contem exatamente os K maiores.</p>
     *
     * <p><b>Complexidade:</b> {@code O(n log k)} -- muito melhor que ordenar todos
     * os n alunos ({@code O(n log n)}) quando k << n.</p>
     *
     * @param k numero de posicoes desejadas (ex: 10)
     */
    @Cacheable(value = "ranking", key = "#k")
    public List<RankingItemDTO> topK(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k deve ser positivo");
        }

        // MIN-HEAP: o menor elemento (menor media) fica na raiz, para ser
        // descartado quando o heap excede k posicoes.
        PriorityQueue<RankingItemDTO> heap =
                new PriorityQueue<>(Comparator.comparingDouble(RankingItemDTO::mediaGeral));

        desempenhoRepo.findMediasPorAluno().forEach(media -> {
            RankingItemDTO item = new RankingItemDTO(
                    0,
                    media.nome(),
                    media.matricula(),
                    media.mediaGeral() != null ? media.mediaGeral() : 0.0,
                    media.disciplinasConcluidas() != null ? media.disciplinasConcluidas().intValue() : 0);
            heap.offer(item);
            if (heap.size() > k) {
                heap.poll();   // remove o de MENOR media -- mantem os maiores
            }
        });

        // Extrair em ordem decrescente (maior media primeiro)
        List<RankingItemDTO> ranking = new ArrayList<>(heap);
        ranking.sort(Comparator.comparingDouble(RankingItemDTO::mediaGeral).reversed());

        // Atribuir as posicoes (1..n)
        List<RankingItemDTO> resultado = new ArrayList<>(ranking.size());
        for (int i = 0; i < ranking.size(); i++) {
            RankingItemDTO item = ranking.get(i);
            resultado.add(new RankingItemDTO(
                    i + 1,
                    item.nome(),
                    item.matricula(),
                    item.mediaGeral(),
                    item.disciplinasConcluidas()));
        }
        return Collections.unmodifiableList(resultado);
    }

    /** Invalida o cache de ranking ao alterar qualquer nota. */
    @CacheEvict(value = "ranking", allEntries = true)
    public void invalidarRanking() {
        // O corpo vazio e proposital: a anotacao @CacheEvict faz todo o trabalho.
    }
}
