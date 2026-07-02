package br.edu.iftm.edumetrics.messaging;

import br.edu.iftm.edumetrics.config.RabbitMQConfig;
import br.edu.iftm.edumetrics.domain.dto.EventoRelatorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RelatorioProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RelatorioProducer producer;

    @Captor
    private ArgumentCaptor<EventoRelatorio> eventoCaptor;

    @Test
    @DisplayName("Publica a mensagem no RabbitTemplate e retorna um correlationId nao-nulo")
    void publicaMensagemERetornaCorrelationId() {
        String correlationId = producer.solicitarRelatorio(42L, "BOLETIM", "2026/1");

        assertNotNull(correlationId);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_RELATORIOS),
                eq(RabbitMQConfig.ROUTING_KEY_PDF),
                eventoCaptor.capture());

        EventoRelatorio evento = eventoCaptor.getValue();
        assertEquals(42L, evento.alunoId());
        assertEquals("BOLETIM", evento.tipo());
        assertEquals("2026/1", evento.semestre());
        assertEquals(correlationId, evento.correlationId());
        assertNotNull(evento.solicitadoEm());
    }
}
