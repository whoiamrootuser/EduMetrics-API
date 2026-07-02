package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.dto.MediaAlunoDTO;
import br.edu.iftm.edumetrics.domain.dto.RankingItemDTO;
import br.edu.iftm.edumetrics.repository.DesempenhoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private DesempenhoRepository desempenhoRepo;

    @InjectMocks
    private RankingService service;

    private List<MediaAlunoDTO> medias() {
        return List.of(
                new MediaAlunoDTO("Ana", "001", 9.5, 5L),
                new MediaAlunoDTO("Bruno", "002", 7.0, 4L),
                new MediaAlunoDTO("Carla", "003", 8.2, 6L),
                new MediaAlunoDTO("Diego", "004", 6.1, 3L),
                new MediaAlunoDTO("Eduarda", "005", 9.9, 5L)
        );
    }

    @Test
    @DisplayName("Top-3 retorna os 3 com maior media, em ordem decrescente e com posicoes")
    void top3RetornaMaioresMedias() {
        when(desempenhoRepo.findMediasPorAluno()).thenReturn(medias());

        List<RankingItemDTO> ranking = service.topK(3);

        assertEquals(3, ranking.size());
        assertEquals("Eduarda", ranking.get(0).nome());   // 9.9
        assertEquals("Ana", ranking.get(1).nome());       // 9.5
        assertEquals("Carla", ranking.get(2).nome());     // 8.2
        assertEquals(1, ranking.get(0).posicao());
        assertEquals(2, ranking.get(1).posicao());
        assertEquals(3, ranking.get(2).posicao());
    }

    @Test
    @DisplayName("k=1 retorna apenas o melhor aluno")
    void k1RetornaApenasOMelhor() {
        when(desempenhoRepo.findMediasPorAluno()).thenReturn(medias());

        List<RankingItemDTO> ranking = service.topK(1);

        assertEquals(1, ranking.size());
        assertEquals("Eduarda", ranking.get(0).nome());
    }

    @Test
    @DisplayName("Empate de medias e tratado sem excecao")
    void empateTratadoSemExcecao() {
        when(desempenhoRepo.findMediasPorAluno()).thenReturn(List.of(
                new MediaAlunoDTO("X", "010", 8.0, 4L),
                new MediaAlunoDTO("Y", "011", 8.0, 4L),
                new MediaAlunoDTO("Z", "012", 8.0, 4L)));

        List<RankingItemDTO> ranking = service.topK(2);
        assertEquals(2, ranking.size());
        assertEquals(8.0, ranking.get(0).mediaGeral());
        assertEquals(8.0, ranking.get(1).mediaGeral());
    }

    @Test
    @DisplayName("k invalido (<= 0) lanca IllegalArgumentException")
    void kInvalidoLanca() {
        assertThrows(IllegalArgumentException.class, () -> service.topK(0));
        assertThrows(IllegalArgumentException.class, () -> service.topK(-1));
    }

    @Test
    @DisplayName("medias nulas (sem notas) sao tratadas como zero")
    void mediaNulaTratadaComoZero() {
        when(desempenhoRepo.findMediasPorAluno()).thenReturn(List.of(
                new MediaAlunoDTO("SemNota", "020", null, null),
                new MediaAlunoDTO("ComNota", "021", 7.0, 3L)));

        List<RankingItemDTO> ranking = service.topK(2);
        assertEquals("ComNota", ranking.get(0).nome());
        assertEquals(0.0, ranking.get(1).mediaGeral());
        assertEquals(0, ranking.get(1).disciplinasConcluidas());
    }

    @Test
    @DisplayName("invalidarRanking() executa sem erro (contrato @CacheEvict do ranking)")
    void invalidarRankingNaoLanca() {
        // A invalidacao real do cache Redis e verificada em CacheIntegrationTest;
        // aqui garantimos o cenario da Secao 8.1 no nivel unitario.
        assertDoesNotThrow(() -> service.invalidarRanking());
    }
}
