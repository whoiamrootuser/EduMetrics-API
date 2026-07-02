package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.domain.dto.AlunoDTO;
import br.edu.iftm.edumetrics.exception.AlunoNaoEncontradoException;
import br.edu.iftm.edumetrics.exception.ConflitoException;
import br.edu.iftm.edumetrics.repository.AlunoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    @Mock
    private AlunoRepository repository;

    @Mock
    private AutocompletarService autocompletar;

    @InjectMocks
    private AlunoService service;

    private Aluno alunoExemplo() {
        Aluno a = new Aluno("20260001", "Ana Souza", "ana@iftm.edu.br", "TSI", 4);
        a.setId(1L);
        return a;
    }

    @Test
    @DisplayName("Cache HIT por matricula: a segunda chamada nao vai ao banco")
    void cacheHitMatriculaNaoVaiAoBanco() {
        Aluno a = alunoExemplo();
        when(repository.findByMatricula("20260001")).thenReturn(Optional.of(a));

        AlunoDTO primeira = service.buscarPorMatricula("20260001");
        AlunoDTO segunda = service.buscarPorMatricula("20260001");   // deve vir do LRUCache local

        assertEquals(primeira, segunda);
        verify(repository, times(1)).findByMatricula("20260001");    // banco consultado UMA vez
    }

    @Test
    @DisplayName("LRUCache local: remover() limpa a entrada; nova busca por matricula volta ao banco")
    void cacheEvictAposRemocao() {
        Aluno a = alunoExemplo();
        when(repository.findByMatricula("20260001")).thenReturn(Optional.of(a));
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        service.buscarPorMatricula("20260001");   // popula o cache local (miss -> banco)
        service.remover(1L);                       // deve limpar o cache local da matricula
        service.buscarPorMatricula("20260001");    // miss de novo -> banco

        verify(repository, times(2)).findByMatricula("20260001");
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Excecao quando o aluno nao existe (busca por id)")
    void excecaoQuandoAlunoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AlunoNaoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    @Test
    @DisplayName("Ao salvar, o nome do aluno e indexado na Trie")
    void salvarIndexaNaTrie() {
        AlunoDTO entrada = new AlunoDTO(null, "20260002", "Bruno Lima",
                "bruno@iftm.edu.br", "TSI", 4);
        when(repository.existsByMatricula("20260002")).thenReturn(false);
        when(repository.existsByEmail("bruno@iftm.edu.br")).thenReturn(false);
        when(repository.save(any(Aluno.class))).thenAnswer(inv -> {
            Aluno salvo = inv.getArgument(0);
            salvo.setId(2L);
            return salvo;
        });

        AlunoDTO salvo = service.salvar(entrada);

        assertEquals(2L, salvo.id());
        verify(autocompletar, times(1)).indexar("Bruno Lima");
    }

    @Test
    @DisplayName("Salvar com matricula duplicada lanca ConflitoException e nao indexa")
    void salvarMatriculaDuplicadaLancaConflito() {
        AlunoDTO entrada = new AlunoDTO(null, "20260001", "Ana Souza",
                "ana@iftm.edu.br", "TSI", 4);
        when(repository.existsByMatricula("20260001")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> service.salvar(entrada));
        verify(autocompletar, never()).indexar(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: altera os campos (mantendo a matricula) e re-indexa a Trie")
    void atualizarMantendoMatricula() {
        Aluno existente = alunoExemplo();   // id 1, matricula 20260001
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Aluno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlunoDTO dto = new AlunoDTO(1L, "20260001", "Ana Souza Silva",
                "ana.silva@iftm.edu.br", "TSI", 5);
        AlunoDTO atualizado = service.atualizar(1L, dto);

        assertEquals("Ana Souza Silva", atualizado.nome());
        assertEquals("ana.silva@iftm.edu.br", atualizado.email());
        assertEquals(5, atualizado.periodo());
        assertEquals("20260001", atualizado.matricula());
        verify(autocompletar, times(1)).indexar("Ana Souza Silva");
    }

    @Test
    @DisplayName("atualizar: trocar a matricula invalida a chave antiga no cache local")
    void atualizarTrocandoMatricula() {
        Aluno existente = alunoExemplo();
        when(repository.findByMatricula("20260001")).thenReturn(Optional.of(existente));
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Aluno.class))).thenAnswer(inv -> inv.getArgument(0));

        service.buscarPorMatricula("20260001");   // popula o cache local com a matricula antiga

        AlunoDTO dto = new AlunoDTO(1L, "20260099", "Ana Souza",
                "ana@iftm.edu.br", "TSI", 4);
        AlunoDTO atualizado = service.atualizar(1L, dto);

        assertEquals("20260099", atualizado.matricula());
        // a chave antiga foi removida do cache local -> nova busca vai ao banco de novo
        service.buscarPorMatricula("20260001");
        verify(repository, times(2)).findByMatricula("20260001");
    }

    @Test
    @DisplayName("atualizar: aluno inexistente lanca excecao")
    void atualizarAlunoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        AlunoDTO dto = new AlunoDTO(99L, "20260099", "X", "x@iftm.edu.br", "TSI", 1);
        assertThrows(AlunoNaoEncontradoException.class, () -> service.atualizar(99L, dto));
    }

    @Test
    @DisplayName("cacheStats reflete hits e misses")
    void cacheStatsRefleteHitsMisses() {
        Aluno a = alunoExemplo();
        when(repository.findByMatricula("20260001")).thenReturn(Optional.of(a));

        service.buscarPorMatricula("20260001");   // miss
        service.buscarPorMatricula("20260001");   // hit

        var stats = service.cacheStats();
        assertEquals(1L, stats.get("hits"));
        assertEquals(1L, stats.get("misses"));
        assertEquals(0.5, stats.get("hitRate"));
    }
}
