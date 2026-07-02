package br.edu.iftm.edumetrics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao da mensageria RabbitMQ: exchange, fila principal, DLQ e
 * conversor JSON. Os nomes sao centralizados em constantes para evitar typos.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_RELATORIOS = "edumetrics.relatorios";
    public static final String FILA_RELATORIOS = "relatorios.processamento";
    public static final String FILA_DLQ = "relatorios.dlq";
    public static final String ROUTING_KEY_PDF = "relatorio.gerar";

    // ── Exchange ────────────────────────────────────────────────────────
    @Bean
    public TopicExchange exchangeRelatorios() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_RELATORIOS)
                .durable(true)   // sobrevive a reinicializacoes do broker
                .build();
    }

    // ── Dead Letter Queue (DLQ) ─────────────────────────────────────────
    @Bean
    public Queue filaDLQ() {
        return QueueBuilder.durable(FILA_DLQ).build();
    }

    // ── Fila principal com referencia a DLQ ─────────────────────────────
    @Bean
    public Queue filaRelatorios() {
        return QueueBuilder
                .durable(FILA_RELATORIOS)
                // Mensagens rejeitadas (apos esgotar as tentativas) vao para a DLQ
                // via exchange default ("") com routing key = nome da fila DLQ.
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", FILA_DLQ)
                .build();
    }

    @Bean
    public Binding bindingRelatorios(Queue filaRelatorios, TopicExchange exchangeRelatorios) {
        return BindingBuilder
                .bind(filaRelatorios)
                .to(exchangeRelatorios)
                .with(ROUTING_KEY_PDF);
    }

    // ── Conversor JSON ──────────────────────────────────────────────────
    // Reutiliza o ObjectMapper do Spring (que ja tem o JavaTimeModule
    // registrado) para serializar o campo Instant de EventoRelatorio.
    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}
