package br.edu.iftm.edumetrics.estruturas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiter com algoritmo Sliding Window.
 *
 * <p>Cada cliente tem um {@code Deque<Long>} de timestamps de requisicoes. Ao
 * processar uma nova requisicao, timestamps fora da janela sao removidos da
 * frente do deque e o tamanho atual e comparado ao limite. E mais preciso que o
 * Fixed Window Counter (nao permite o burst de ate 2x no limite da janela).</p>
 *
 * <p><b>Complexidade:</b> {@code permitir} O(1) amortizado por requisicao --
 * cada timestamp e inserido uma vez ({@code addLast}) e removido no maximo uma
 * vez ({@code pollFirst}).</p>
 */
@Component
public class RateLimiter {

    // ConcurrentHashMap garante thread-safety sem bloqueio global entre clientes
    private final ConcurrentHashMap<String, Deque<Long>> janelas = new ConcurrentHashMap<>();

    private final int maxRequisicoes;
    private final long janelaMilissegundos;

    /**
     * Construtor com injecao de configuracao via application.properties.
     * O Spring usa este construtor; testes e benchmarks podem instanciar
     * passando valores diretamente.
     */
    public RateLimiter(@Value("${rate.limiter.max-requisicoes:100}") int maxRequisicoes,
                       @Value("${rate.limiter.janela-ms:60000}") long janelaMilissegundos) {
        this.maxRequisicoes = maxRequisicoes;
        this.janelaMilissegundos = janelaMilissegundos;
    }

    /**
     * Fonte de tempo. Isolada em um metodo {@code protected} para que os testes
     * possam controla-la deterministicamente (sem {@code Thread.sleep}).
     */
    protected long agora() {
        return System.currentTimeMillis();
    }

    /**
     * Verifica se o cliente pode fazer a requisicao.
     *
     * @param clienteId identificador do cliente (IP, userId, API key)
     * @return {@code true} se dentro do limite; {@code false} se excedeu
     */
    public boolean permitir(String clienteId) {
        long agora = agora();
        Deque<Long> timestamps = janelas.computeIfAbsent(clienteId, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps fora da janela deslizante
            while (!timestamps.isEmpty() && agora - timestamps.peekFirst() >= janelaMilissegundos) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequisicoes) {
                return false;   // limite excedido -> 429 Too Many Requests
            }
            timestamps.addLast(agora);
            return true;
        }
    }

    /** Estatisticas para o endpoint /api/admin/rate-limiter/stats. */
    public Map<String, Object> stats(String clienteId) {
        Deque<Long> ts = janelas.getOrDefault(clienteId, new ArrayDeque<>());
        int usadas;
        synchronized (ts) {
            usadas = ts.size();
        }
        return Map.of(
                "cliente", clienteId,
                "requisicoes_usadas", usadas,
                "limite", maxRequisicoes,
                "janela_ms", janelaMilissegundos
        );
    }

    public int getMaxRequisicoes() {
        return maxRequisicoes;
    }

    public long getJanelaMilissegundos() {
        return janelaMilissegundos;
    }
}
