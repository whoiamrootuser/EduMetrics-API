package br.edu.iftm.edumetrics.domain.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Mensagem publicada no RabbitMQ para geracao assincrona de relatorios.
 * Deve ser serializavel (Jackson) para trafegar pelo broker.
 */
public record EventoRelatorio(
        String correlationId,   // UUID para rastrear o relatorio
        Long alunoId,
        String tipo,            // "BOLETIM", "HISTORICO", "RANKING"
        String semestre,
        Instant solicitadoEm
) implements Serializable {
}
