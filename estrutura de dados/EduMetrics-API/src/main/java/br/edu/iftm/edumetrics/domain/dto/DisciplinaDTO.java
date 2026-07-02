package br.edu.iftm.edumetrics.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de Disciplina (entrada e saida).
 */
public record DisciplinaDTO(
        Long id,

        @NotBlank(message = "codigo e obrigatorio")
        @Size(max = 10)
        String codigo,

        @NotBlank(message = "nome e obrigatorio")
        @Size(max = 100)
        String nome,

        @NotNull(message = "creditos e obrigatorio")
        @Positive(message = "creditos deve ser positivo")
        Integer creditos
) {
}
