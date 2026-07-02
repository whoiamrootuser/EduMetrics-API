package br.edu.iftm.edumetrics.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisicao POST /api/relatorios.
 */
public record SolicitacaoRelatorioDTO(
        @NotNull(message = "alunoId e obrigatorio")
        Long alunoId,

        @NotBlank(message = "tipo e obrigatorio (BOLETIM, HISTORICO ou RANKING)")
        String tipo,

        @NotBlank(message = "semestre e obrigatorio")
        String semestre
) {
}
