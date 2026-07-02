package br.edu.iftm.edumetrics.controller;

import br.edu.iftm.edumetrics.estruturas.RateLimiter;
import br.edu.iftm.edumetrics.service.AlunoService;
import br.edu.iftm.edumetrics.service.AutocompletarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints administrativos: estatisticas de cache, limpeza de cache,
 * estatisticas do rate limiter e health de Redis/RabbitMQ/Trie.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Estatisticas de cache, rate limiter e health")
public class AdminController {

    private final AlunoService alunoService;
    private final RateLimiter rateLimiter;
    private final AutocompletarService autocompletarService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ConnectionFactory rabbitConnectionFactory;

    public AdminController(AlunoService alunoService,
                           RateLimiter rateLimiter,
                           AutocompletarService autocompletarService,
                           RedisConnectionFactory redisConnectionFactory,
                           ConnectionFactory rabbitConnectionFactory) {
        this.alunoService = alunoService;
        this.rateLimiter = rateLimiter;
        this.autocompletarService = autocompletarService;
        this.redisConnectionFactory = redisConnectionFactory;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "Estatisticas do LRUCache local (hits, misses, hitRate, entradas)")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        return ResponseEntity.ok(alunoService.cacheStats());
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Limpa todos os caches de alunos (@CacheEvict allEntries)")
    public ResponseEntity<Void> limparCache() {
        alunoService.limparCache();
        return ResponseEntity.noContent().build();   // 204
    }

    @GetMapping("/rate-limiter/stats")
    @Operation(summary = "Estatisticas do rate limiter para o IP da requisicao")
    public ResponseEntity<Map<String, Object>> rateLimiterStats(HttpServletRequest request) {
        return ResponseEntity.ok(rateLimiter.stats(extrairClienteId(request)));
    }

    @GetMapping("/health")
    @Operation(summary = "Status de Redis, RabbitMQ e total de termos na Trie")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("redis", pingRedis());
        status.put("rabbitmq", pingRabbit());
        status.put("trie_palavras_indexadas", autocompletarService.totalIndexado());
        return ResponseEntity.ok(status);
    }

    private String pingRedis() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String pong = conn.ping();
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "UP (" + pong + ")";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String pingRabbit() {
        Connection conn = null;
        try {
            conn = rabbitConnectionFactory.createConnection();
            return conn.isOpen() ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                    // best-effort
                }
            }
        }
    }

    private String extrairClienteId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
