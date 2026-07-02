package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.domain.Desempenho;
import br.edu.iftm.edumetrics.domain.Disciplina;
import br.edu.iftm.edumetrics.domain.dto.DesempenhoDTO;
import br.edu.iftm.edumetrics.domain.dto.RegistroDesempenhoDTO;
import br.edu.iftm.edumetrics.exception.AlunoNaoEncontradoException;
import br.edu.iftm.edumetrics.exception.DisciplinaNaoEncontradaException;
import br.edu.iftm.edumetrics.repository.AlunoRepository;
import br.edu.iftm.edumetrics.repository.DesempenhoRepository;
import br.edu.iftm.edumetrics.repository.DisciplinaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DesempenhoServiceTest {

    @Mock
    private DesempenhoRepository desempenhoRepo;
    @Mock
    private AlunoRepository alunoRepo;
    @Mock
    private DisciplinaRepository disciplinaRepo;
    @Mock
    private RankingService rankingService;

    @InjectMocks
    private DesempenhoService service;

    private Aluno aluno() {
        Aluno a = new Aluno("20260001", "Ana", "ana@iftm.edu.br", "TSI", 4);
        a.setId(1L);
        return a;
    }

    private Disciplina disciplina() {
        Disciplina d = new Disciplina("ED01", "Estrutura de Dados", 4);
        d.setId(1L);
        return d;
    }

    @Test
    @DisplayName("Registrar calcula a nota final, salva e invalida o ranking")
    void registrarCalculaNotaFinalEInvalidaRanking() {
        RegistroDesempenhoDTO req = new RegistroDesempenhoDTO(
                1L, 1L, new BigDecimal("8.0"), new BigDecimal("6.0"), "2026/1");
        when(alunoRepo.findById(1L)).thenReturn(Optional.of(aluno()));
        when(disciplinaRepo.findById(1L)).thenReturn(Optional.of(disciplina()));
        when(desempenhoRepo.save(any(Desempenho.class))).thenAnswer(inv -> inv.getArgument(0));

        DesempenhoDTO dto = service.registrar(req);

        assertEquals("Estrutura de Dados", dto.disciplina());
        assertEquals(0, new BigDecimal("7.00").compareTo(dto.notaFinal()));   // (8 + 6) / 2
        verify(rankingService, times(1)).invalidarRanking();
    }

    @Test
    @DisplayName("Registrar para aluno inexistente lanca excecao")
    void registrarAlunoInexistente() {
        RegistroDesempenhoDTO req = new RegistroDesempenhoDTO(
                99L, 1L, new BigDecimal("8.0"), new BigDecimal("6.0"), "2026/1");
        when(alunoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AlunoNaoEncontradoException.class, () -> service.registrar(req));
    }

    @Test
    @DisplayName("Registrar para disciplina inexistente lanca excecao")
    void registrarDisciplinaInexistente() {
        RegistroDesempenhoDTO req = new RegistroDesempenhoDTO(
                1L, 99L, new BigDecimal("8.0"), new BigDecimal("6.0"), "2026/1");
        when(alunoRepo.findById(1L)).thenReturn(Optional.of(aluno()));
        when(disciplinaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DisciplinaNaoEncontradaException.class, () -> service.registrar(req));
    }

    @Test
    @DisplayName("Listar desempenho do aluno retorna os DTOs (JOIN FETCH, sem N+1)")
    void listarPorAluno() {
        Desempenho d = new Desempenho(aluno(), disciplina(),
                new BigDecimal("9.0"), new BigDecimal("7.0"), "2026/1");
        when(alunoRepo.existsById(1L)).thenReturn(true);
        when(desempenhoRepo.findByAlunoIdFetchDisciplina(1L)).thenReturn(List.of(d));

        List<DesempenhoDTO> lista = service.listarPorAluno(1L);

        assertEquals(1, lista.size());
        assertEquals("Estrutura de Dados", lista.get(0).disciplina());
        assertEquals(0, new BigDecimal("8.00").compareTo(lista.get(0).notaFinal()));
    }

    @Test
    @DisplayName("Listar desempenho de aluno inexistente lanca excecao")
    void listarPorAlunoInexistente() {
        when(alunoRepo.existsById(99L)).thenReturn(false);
        assertThrows(AlunoNaoEncontradoException.class, () -> service.listarPorAluno(99L));
    }
}
