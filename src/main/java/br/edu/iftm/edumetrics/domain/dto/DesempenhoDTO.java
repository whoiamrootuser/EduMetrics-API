package br.edu.iftm.edumetrics.domain.dto;

import java.math.BigDecimal;

/**
 * DTO de saida para a nota de um aluno em uma disciplina.
 * Imutavel -- usado tambem como valor de cache ("desempenhos").
 */
public record DesempenhoDTO(
        String disciplina,
        BigDecimal nota1,
        BigDecimal nota2,
        BigDecimal notaFinal,
        String semestre
) {
}
