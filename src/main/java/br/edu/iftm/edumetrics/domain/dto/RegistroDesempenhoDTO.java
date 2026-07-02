package br.edu.iftm.edumetrics.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Corpo da requisicao POST /api/desempenhos (registrar nota).
 */
public record RegistroDesempenhoDTO(
        @NotNull(message = "alunoId e obrigatorio")
        Long alunoId,

        @NotNull(message = "disciplinaId e obrigatorio")
        Long disciplinaId,

        @NotNull(message = "nota1 e obrigatoria")
        @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
        BigDecimal nota1,

        @NotNull(message = "nota2 e obrigatoria")
        @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
        BigDecimal nota2,

        @NotBlank(message = "semestre e obrigatorio")
        String semestre
) {
}
