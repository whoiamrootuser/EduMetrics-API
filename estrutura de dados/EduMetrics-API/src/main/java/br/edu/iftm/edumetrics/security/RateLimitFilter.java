package br.edu.iftm.edumetrics.security;

import br.edu.iftm.edumetrics.estruturas.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP que aplica o {@link RateLimiter} (Sliding Window) a todos os
 * endpoints da API. Excede o limite -> HTTP 429 Too Many Requests.
 *
 * <p>Caminhos de infraestrutura (H2 console, Swagger, Actuator) sao isentos para
 * nao atrapalhar a demonstracao.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.equals("/error")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clienteId = extrairClienteId(request);
        if (!rateLimiter.permitir(clienteId)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());   // 429
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"erro\": \"Rate limit excedido\", \"limite\": %d, \"janela_ms\": %d}"
                            .formatted(rateLimiter.getMaxRequisicoes(), rateLimiter.getJanelaMilissegundos()));
            return;
        }
        chain.doFilter(request, response);
    }

    private String extrairClienteId(HttpServletRequest request) {
        // Tenta X-Forwarded-For (proxy/load balancer); cai no IP direto
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
