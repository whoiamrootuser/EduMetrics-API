package br.edu.iftm.edumetrics;

import br.edu.iftm.edumetrics.repository.AlunoRepository;
import br.edu.iftm.edumetrics.repository.DesempenhoRepository;
import br.edu.iftm.edumetrics.service.AutocompletarService;
import br.edu.iftm.edumetrics.service.RankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao leve: sobe o contexto Spring inteiro (sem Docker, usando
 * o perfil de teste com cache em memoria e listeners do RabbitMQ desligados).
 *
 * <p>Valida o wiring de todos os beans, o mapeamento JPA, a carga inicial do
 * {@code DataLoader} e a populacao da Trie pela {@code AutocompletarService}.</p>
 */
@SpringBootTest
class EduMetricsApplicationTests {

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private DesempenhoRepository desempenhoRepository;

    @Autowired
    private AutocompletarService autocompletarService;

    @Autowired
    private RankingService rankingService;

    @Test
    @DisplayName("O contexto Spring sobe e o DataLoader semeia os dados")
    void contextLoadsESemeiaDados() {
        assertTrue(alunoRepository.count() > 0, "DataLoader deve ter criado alunos");
        assertTrue(desempenhoRepository.count() > 0, "DataLoader deve ter criado desempenhos");
    }

    @Test
    @DisplayName("A Trie e carregada no startup com as disciplinas semeadas")
    void trieCarregadaNoStartup() {
        assertTrue(autocompletarService.totalIndexado() > 0, "Trie deve ter termos indexados");
        assertFalse(autocompletarService.sugerir("Estr").isEmpty(),
                "autocompletar de 'Estr' deve encontrar 'Estrutura de Dados'");
    }

    @Test
    @DisplayName("O ranking e calculado a partir dos dados semeados")
    void rankingFunciona() {
        assertFalse(rankingService.topK(5).isEmpty(), "ranking nao deve ser vazio");
    }
}
