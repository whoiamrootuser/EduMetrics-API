package br.edu.iftm.edumetrics.messaging;

import br.edu.iftm.edumetrics.config.RabbitMQConfig;
import br.edu.iftm.edumetrics.domain.dto.EventoRelatorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica eventos de geracao de relatorio no RabbitMQ e retorna imediatamente,
 * sem esperar o processamento (resposta HTTP 202 Accepted).
 */
@Service
public class RelatorioProducer {

    private static final Logger log = LoggerFactory.getLogger(RelatorioProducer.class);

    private final RabbitTemplate rabbitTemplate;

    /**
     * Habilita o envio real ao broker. No perfil "local" (sem Docker) e definido
     * como {@code false}, e o envio e apenas simulado (o app roda sem RabbitMQ).
     */
    @Value("${edumetrics.mensageria.habilitada:true}")
    private boolean mensageriaHabilitada = true;

    public RelatorioProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public String solicitarRelatorio(Long alunoId, String tipo, String semestre) {
        String correlationId = UUID.randomUUID().toString();
        EventoRelatorio evento = new EventoRelatorio(
                correlationId, alunoId, tipo, semestre, Instant.now());

        if (!mensageriaHabilitada) {
            // Perfil local (sem RabbitMQ): apenas simula o envio para nao quebrar o endpoint.
            log.warn("Mensageria desabilitada (perfil local) -- relatorio {} SIMULADO, correlationId={}",
                    tipo, correlationId);
            return correlationId;
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_RELATORIOS,
                RabbitMQConfig.ROUTING_KEY_PDF,
                evento);

        log.info("Relatorio {} solicitado -- correlationId={}", tipo, correlationId);
        return correlationId;   // retornado ao controller (HTTP 202 Accepted)
    }
}
