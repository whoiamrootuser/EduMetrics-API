package br.edu.iftm.edumetrics.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de Aluno.
 *
 * <p>Records sao imutaveis -- ideais para cache (sem risco de mutacao apos
 * armazenar no Redis ou no LRUCache local).</p>
 *
 * <p>O campo {@code id} e nulo na criacao (POST) e preenchido nas respostas.</p>
 */
public record AlunoDTO(
        Long id,

        @NotBlank(message = "matricula e obrigatoria")
        @Size(max = 12, message = "matricula deve ter no maximo 12 caracteres")
        String matricula,

        @NotBlank(message = "nome e obrigatorio")
        @Size(max = 120)
        String nome,

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email invalido")
        String email,

        @NotBlank(message = "curso e obrigatorio")
        String curso,

        @NotNull(message = "periodo e obrigatorio")
        @Positive(message = "periodo deve ser positivo")
        Integer periodo
) {
}
