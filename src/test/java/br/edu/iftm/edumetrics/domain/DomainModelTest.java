package br.edu.iftm.edumetrics.domain;

import br.edu.iftm.edumetrics.domain.dto.DisciplinaDTO;
import br.edu.iftm.edumetrics.domain.dto.RankingItemDTO;
import br.edu.iftm.edumetrics.domain.dto.SolicitacaoRelatorioDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do modelo de dominio: entidades (getters/setters, equals/hashCode,
 * regras de calculo) e DTOs.
 */
class DomainModelTest {

    // ───────────────────────── Aluno ─────────────────────────
    @Test
    @DisplayName("Aluno: getters/setters e adicionarDesempenho")
    void alunoGettersSettersEDesempenho() {
        Aluno a = new Aluno("20260001", "Ana", "ana@iftm.edu.br", "TSI", 4);
        a.setId(1L);
        a.setMatricula("20260002");
        a.setNome("Ana Souza");
        a.setEmail("ana2@iftm.edu.br");
        a.setCurso("TSI-N");
        a.setPeriodo(5);

        assertEquals(1L, a.getId());
        assertEquals("20260002", a.getMatricula());
        assertEquals("Ana Souza", a.getNome());
        assertEquals("ana2@iftm.edu.br", a.getEmail());
        assertEquals("TSI-N", a.getCurso());
        assertEquals(5, a.getPeriodo());
        assertTrue(a.toString().contains("20260002"));

        Disciplina d = new Disciplina("ED01", "Estrutura de Dados", 4);
        Desempenho desemp = new Desempenho(null, d, new BigDecimal("8"), new BigDecimal("6"), "2026/1");
        a.adicionarDesempenho(desemp);
        assertEquals(1, a.getDesempenhos().size());
        assertSame(a, desemp.getAluno());
    }

    @Test
    @DisplayName("Aluno: equals/hashCode por id")
    void alunoEqualsHashCode() {
        Aluno a1 = new Aluno("1", "A", "a@x", "TSI", 1);
        a1.setId(1L);
        Aluno a2 = new Aluno("2", "B", "b@x", "TSI", 1);
        a2.setId(1L);
        Aluno a3 = new Aluno("3", "C", "c@x", "TSI", 1);
        a3.setId(2L);

        assertEquals(a1, a1);                 // mesma instancia
        assertEquals(a1, a2);                 // mesmo id
        assertNotEquals(a1, a3);              // id diferente
        assertNotEquals(a1, null);            // null
        assertNotEquals(a1, "string");        // tipo diferente
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    // ───────────────────────── Disciplina ─────────────────────────
    @Test
    @DisplayName("Disciplina: getters/setters, equals/hashCode e toString")
    void disciplina() {
        Disciplina d = new Disciplina("ED01", "Estrutura de Dados", 4);
        d.setId(10L);
        d.setCodigo("ED02");
        d.setNome("ED Avancada");
        d.setCreditos(6);

        assertEquals(10L, d.getId());
        assertEquals("ED02", d.getCodigo());
        assertEquals("ED Avancada", d.getNome());
        assertEquals(6, d.getCreditos());
        assertTrue(d.toString().contains("ED02"));

        Disciplina outra = new Disciplina("X", "Y", 1);
        outra.setId(10L);
        assertEquals(d, outra);
        assertEquals(d, d);
        assertNotEquals(d, null);
        assertNotEquals(d, new Object());
        assertEquals(d.hashCode(), outra.hashCode());
    }

    // ───────────────────────── Desempenho ─────────────────────────
    @Test
    @DisplayName("Desempenho: media das duas notas")
    void desempenhoMediaDuasNotas() {
        Desempenho d = new Desempenho(null, null, new BigDecimal("8.0"), new BigDecimal("6.0"), "2026/1");
        assertEquals(0, new BigDecimal("7.00").compareTo(d.getNotaFinal()));
    }

    @Test
    @DisplayName("Desempenho: uma nota ausente conta como zero")
    void desempenhoUmaNotaAusente() {
        Desempenho d = new Desempenho(null, null, new BigDecimal("8.0"), null, "2026/1");
        assertEquals(0, new BigDecimal("4.00").compareTo(d.getNotaFinal()));
    }

    @Test
    @DisplayName("Desempenho: nota1 ausente e nota2 presente conta nota1 como zero")
    void desempenhoNota1Ausente() {
        Desempenho d = new Desempenho(null, null, null, new BigDecimal("6.0"), "2026/1");
        assertEquals(0, new BigDecimal("3.00").compareTo(d.getNotaFinal()));
    }

    @Test
    @DisplayName("Desempenho: ambas as notas ausentes -> nota final nula")
    void desempenhoAmbasAusentes() {
        Desempenho d = new Desempenho(null, null, null, null, "2026/1");
        assertNull(d.getNotaFinal());
    }

    @Test
    @DisplayName("Desempenho: setNota1/setNota2 recalculam a nota final")
    void desempenhoSettersRecalculam() {
        Desempenho d = new Desempenho(null, null, new BigDecimal("5.0"), new BigDecimal("5.0"), "2026/1");
        d.setId(7L);
        d.setNota1(new BigDecimal("10.0"));
        d.setNota2(new BigDecimal("8.0"));
        d.setSemestre("2026/2");

        assertEquals(7L, d.getId());
        assertEquals(0, new BigDecimal("9.00").compareTo(d.getNotaFinal()));
        assertEquals(new BigDecimal("10.0"), d.getNota1());
        assertEquals(new BigDecimal("8.0"), d.getNota2());
        assertEquals("2026/2", d.getSemestre());
    }

    @Test
    @DisplayName("Desempenho: equals/hashCode por id")
    void desempenhoEqualsHashCode() {
        Desempenho d1 = new Desempenho(null, null, null, null, "2026/1");
        d1.setId(1L);
        Desempenho d2 = new Desempenho(null, null, null, null, "2026/1");
        d2.setId(1L);
        Desempenho d3 = new Desempenho(null, null, null, null, "2026/1");
        d3.setId(2L);

        assertEquals(d1, d2);
        assertEquals(d1, d1);
        assertNotEquals(d1, d3);
        assertNotEquals(d1, null);
        assertNotEquals(d1, "x");
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    // ───────────────────────── DTOs ─────────────────────────
    @Test
    @DisplayName("RankingItemDTO.compareTo ordena por media crescente")
    void rankingItemCompareTo() {
        RankingItemDTO menor = new RankingItemDTO(1, "A", "001", 7.0, 3);
        RankingItemDTO maior = new RankingItemDTO(2, "B", "002", 9.0, 4);
        assertTrue(menor.compareTo(maior) < 0);
        assertTrue(maior.compareTo(menor) > 0);
        assertEquals(0, menor.compareTo(new RankingItemDTO(3, "C", "003", 7.0, 1)));
        assertEquals(9.0, maior.mediaGeral());
    }

    @Test
    @DisplayName("Records DisciplinaDTO e SolicitacaoRelatorioDTO expoem os campos")
    void recordsAcessores() {
        DisciplinaDTO dd = new DisciplinaDTO(1L, "ED01", "Estrutura de Dados", 4);
        assertEquals("ED01", dd.codigo());
        assertEquals("Estrutura de Dados", dd.nome());
        assertEquals(4, dd.creditos());

        SolicitacaoRelatorioDTO sol = new SolicitacaoRelatorioDTO(5L, "BOLETIM", "2026/1");
        assertEquals(5L, sol.alunoId());
        assertEquals("BOLETIM", sol.tipo());
        assertEquals("2026/1", sol.semestre());
    }
}
