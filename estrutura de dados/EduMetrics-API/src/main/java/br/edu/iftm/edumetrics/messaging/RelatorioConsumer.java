package br.edu.iftm.edumetrics.messaging;

import br.edu.iftm.edumetrics.config.RabbitMQConfig;
import br.edu.iftm.edumetrics.domain.dto.EventoRelatorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Consome os eventos de relatorio em uma thread de background.
 *
 * <p>Tipos validos: BOLETIM, HISTORICO, RANKING. Um tipo invalido (ex: "ERRO")
 * lanca excecao -- apos esgotar as tentativas de reentrega, a mensagem e
 * encaminhada para a DLQ ({@code relatorios.dlq}). Esse e o gatilho usado para
 * demonstrar o fluxo de Dead Letter Queue.</p>
 */
@Component
public class RelatorioConsumer {

    private static final Logger log = LoggerFactory.getLogger(RelatorioConsumer.class);
    private static final Set<String> TIPOS_VALIDOS = Set.of("BOLETIM", "HISTORICO", "RANKING");

    @RabbitListener(queues = RabbitMQConfig.FILA_RELATORIOS)
    public void processar(EventoRelatorio evento) {
        log.info("Processando relatorio {} para aluno {} (correlationId={})",
                evento.tipo(), evento.alunoId(), evento.correlationId());

        if (evento.tipo() == null || !TIPOS_VALIDOS.contains(evento.tipo().toUpperCase())) {
            // Tipo invalido demonstra o caminho de falha -> reentrega -> DLQ
            throw new IllegalArgumentException("Tipo de relatorio invalido: " + evento.tipo());
        }

        try {
            // Simula a geracao de PDF (substituir por iText/JasperReports em producao)
            Thread.sleep(2_000);
            log.info("Relatorio {} concluido -- correlationId={}",
                    evento.tipo(), evento.correlationId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        }
    }

    /** Handler da DLQ -- processa mensagens que falharam apos todas as tentativas. */
    @RabbitListener(queues = RabbitMQConfig.FILA_DLQ)
    public void processarDLQ(EventoRelatorio evento) {
        log.error("Mensagem na DLQ -- relatorio {} para aluno {} NAO foi gerado (correlationId={}).",
                evento.tipo(), evento.alunoId(), evento.correlationId());
        // Aqui: notificar monitoramento, salvar log de falha no BD, etc.
    }
}
