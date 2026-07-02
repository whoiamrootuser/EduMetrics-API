package br.edu.iftm.edumetrics.controller;

import br.edu.iftm.edumetrics.domain.dto.SolicitacaoRelatorioDTO;
import br.edu.iftm.edumetrics.messaging.RelatorioProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de solicitacao assincrona de relatorios (publica no RabbitMQ).
 */
@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatorios", description = "Geracao assincrona de relatorios via RabbitMQ")
public class RelatorioController {

    private final RelatorioProducer producer;

    public RelatorioController(RelatorioProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    @Operation(summary = "Solicitar relatorio -- publica no RabbitMQ e retorna 202 com correlationId")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitacaoRelatorioDTO req) {
        String correlationId = producer.solicitarRelatorio(req.alunoId(), req.tipo(), req.semestre());
        // HTTP 202 Accepted -- nao espera o processamento
        return ResponseEntity.accepted().body(Map.of(
                "correlationId", correlationId,
                "mensagem", "Relatorio em processamento. Use o correlationId para rastrear."));
    }
}
