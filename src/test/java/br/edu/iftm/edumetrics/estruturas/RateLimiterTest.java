package br.edu.iftm.edumetrics.estruturas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    /**
     * Subclasse de teste com relogio controlavel -- evita Thread.sleep e torna
     * os testes deterministicos. {@code agora()} e protected, sobrescrita aqui.
     */
    static class RateLimiterControlado extends RateLimiter {
        long instante = 0L;

        RateLimiterControlado(int max, long janelaMs) {
            super(max, janelaMs);
        }

        @Override
        protected long agora() {
            return instante;
        }
    }

    @Test
    @DisplayName("Deve permitir requisicoes dentro do limite")
    void devePermitirDentroDoLimite() {
        RateLimiter rl = new RateLimiterControlado(3, 1000);
        assertTrue(rl.permitir("ip-1"));
        assertTrue(rl.permitir("ip-1"));
        assertTrue(rl.permitir("ip-1"));
    }

    @Test
    @DisplayName("Deve bloquear a (N+1)-esima requisicao")
    void deveBloquearNMaisUm() {
        RateLimiter rl = new RateLimiterControlado(3, 1000);
        rl.permitir("ip-1");
        rl.permitir("ip-1");
        rl.permitir("ip-1");
        assertFalse(rl.permitir("ip-1"), "a 4a requisicao deve ser bloqueada");
    }

    @Test
    @DisplayName("Limites sao independentes por cliente")
    void limitesIndependentesPorCliente() {
        RateLimiter rl = new RateLimiterControlado(2, 1000);
        assertTrue(rl.permitir("ip-1"));
        assertTrue(rl.permitir("ip-1"));
        assertFalse(rl.permitir("ip-1"));
        // outro cliente comeca do zero
        assertTrue(rl.permitir("ip-2"));
        assertTrue(rl.permitir("ip-2"));
        assertFalse(rl.permitir("ip-2"));
    }

    @Test
    @DisplayName("Apos expirar a janela, deve permitir novamente")
    void aposExpirarJanelaPermiteNovamente() {
        RateLimiterControlado rl = new RateLimiterControlado(2, 1000);
        rl.instante = 0;
        assertTrue(rl.permitir("ip-1"));
        assertTrue(rl.permitir("ip-1"));
        assertFalse(rl.permitir("ip-1"), "limite atingido na janela atual");

        // Avanca o tempo alem da janela (1000 ms)
        rl.instante = 1000;
        assertTrue(rl.permitir("ip-1"), "timestamps antigos expiraram -> permite de novo");
    }

    @Test
    @DisplayName("Reset correto: janela desliza removendo apenas os timestamps expirados")
    void janelaDeslizanteRemoveApenasExpirados() {
        RateLimiterControlado rl = new RateLimiterControlado(2, 1000);
        rl.instante = 0;
        assertTrue(rl.permitir("ip-1"));    // t=0
        rl.instante = 500;
        assertTrue(rl.permitir("ip-1"));    // t=500 -> 2 na janela
        rl.instante = 900;
        assertFalse(rl.permitir("ip-1"));   // ainda 2 na janela [0,500]
        rl.instante = 1000;
        // t=0 expira (>= 1000), t=500 permanece -> ha espaco para 1
        assertTrue(rl.permitir("ip-1"));
    }

    @Test
    @DisplayName("stats() e getters expoem a configuracao e o uso atual do cliente")
    void statsEGetters() {
        RateLimiter rl = new RateLimiterControlado(5, 60_000);
        rl.permitir("ip-9");
        rl.permitir("ip-9");

        var stats = rl.stats("ip-9");
        assertEquals("ip-9", stats.get("cliente"));
        assertEquals(2, stats.get("requisicoes_usadas"));
        assertEquals(5, stats.get("limite"));
        assertEquals(60_000L, stats.get("janela_ms"));

        assertEquals(5, rl.getMaxRequisicoes());
        assertEquals(60_000L, rl.getJanelaMilissegundos());

        // cliente ainda sem requisicoes
        assertEquals(0, rl.stats("ninguem").get("requisicoes_usadas"));
    }
}
