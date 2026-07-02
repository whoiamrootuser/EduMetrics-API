package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Disciplina;
import br.edu.iftm.edumetrics.domain.dto.DisciplinaDTO;
import br.edu.iftm.edumetrics.exception.ConflitoException;
import br.edu.iftm.edumetrics.repository.DisciplinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutocompletarServiceTest {

    @Mock
    private DisciplinaRepository disciplinaRepo;

    private AutocompletarService service;

    @BeforeEach
    void setUp() {
        service = new AutocompletarService(disciplinaRepo);
    }

    @Test
    @DisplayName("carregarDisciplinas popula a Trie a partir do repositorio")
    void carregarDisciplinasPopulaTrie() {
        when(disciplinaRepo.findAll()).thenReturn(List.of(
                new Disciplina("ED01", "Estrutura de Dados", 4),
                new Disciplina("ALG1", "Algoritmos", 4)));

        service.carregarDisciplinas();

        assertEquals(2, service.totalIndexado());
        assertTrue(service.sugerir("Est").contains("Estrutura de Dados"));
        assertTrue(service.sugerir("Alg").contains("Algoritmos"));
    }

    @Test
    @DisplayName("criarDisciplina persiste, indexa na Trie e retorna o DTO")
    void criarDisciplinaPersisteEIndexa() {
        when(disciplinaRepo.existsByCodigo("PWEB")).thenReturn(false);
        when(disciplinaRepo.save(any(Disciplina.class))).thenAnswer(inv -> {
            Disciplina d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });

        DisciplinaDTO salva = service.criarDisciplina(
                new DisciplinaDTO(null, "PWEB", "Programacao Web", 4));

        assertEquals(10L, salva.id());
        assertEquals("PWEB", salva.codigo());
        assertTrue(service.sugerir("Prog").contains("Programacao Web"),
                "a disciplina recem-criada deve ficar disponivel no autocompletar");
    }

    @Test
    @DisplayName("criarDisciplina com codigo duplicado lanca ConflitoException e nao salva")
    void criarDisciplinaDuplicadaLancaConflito() {
        when(disciplinaRepo.existsByCodigo("ED01")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> service.criarDisciplina(
                new DisciplinaDTO(null, "ED01", "Estrutura de Dados", 4)));
        verify(disciplinaRepo, never()).save(any());
    }

    @Test
    @DisplayName("indexar adiciona um termo avulso na Trie")
    void indexarAdicionaTermo() {
        service.indexar("Compiladores");
        assertEquals(1, service.totalIndexado());
        assertTrue(service.sugerir("Comp").contains("Compiladores"));
    }
}
