package br.edu.iftm.edumetrics.estruturas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LRUCacheTest {

    private Map<String, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = LRUCache.create(3);   // capacidade = 3
    }

    @Test
    @DisplayName("Deve armazenar e recuperar um valor")
    void deveArmazenarERecuperar() {
        cache.put("a", 1);
        assertEquals(1, cache.get("a"));
    }

    @Test
    @DisplayName("Deve respeitar a capacidade maxima")
    void deveRespeitarCapacidade() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4);   // "a" deve ser eviccionado (LRU)

        assertEquals(3, cache.size());
        assertNull(cache.get("a"), "a deveria ter sido eviccionado");
        assertEquals(4, cache.get("d"));
    }

    @Test
    @DisplayName("Deve preservar elemento recem-acessado via get()")
    void devePreservarElementoAcessado() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.get("a");      // "a" torna-se o mais recente
        cache.put("d", 4);   // "b" deve ser eviccionado (agora o LRU)

        assertNotNull(cache.get("a"), "a foi acessado -- nao deve ser eviccionado");
        assertNull(cache.get("b"), "b deve ter sido eviccionado");
    }

    @Test
    @DisplayName("Deve remover o LRU correto em uma sequencia de acessos")
    void deveRemoverLruCorretoEmSequencia() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.get("a");      // ordem de acesso (mais antigo->recente): b, c, a
        cache.get("b");      // b, c, a -> c, a, b
        cache.put("d", 4);   // remove "c" (o LRU)

        assertNull(cache.get("c"), "c era o menos recentemente usado");
        assertNotNull(cache.get("a"));
        assertNotNull(cache.get("b"));
        assertNotNull(cache.get("d"));
    }

    @Test
    @DisplayName("Capacidade invalida deve lancar excecao")
    void capacidadeInvalidaDeveLancar() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new LRUCache<String, Integer>(0));
    }

    @Test
    @DisplayName("Deve ser thread-safe sob contencao real de chaves e evicção concorrente")
    void deveSerThreadSafe() throws InterruptedException, ExecutionException {
        // Capacidade pequena + espaco de chaves pequeno => muitas evicções
        // concorrentes. Em um LinkedHashMap NAO sincronizado (accessOrder), isso
        // dispararia ConcurrentModificationException; o synchronizedMap evita.
        final int capacidade = 10;
        final int espacoDeChaves = 20;
        final int threads = 16;
        final int ops = 5_000;
        Map<String, Integer> compartilhado = LRUCache.create(capacidade);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            futuros.add(executor.submit(() -> {
                largada.await();   // todas as threads comecam juntas (maxima contencao)
                for (int i = 0; i < ops; i++) {
                    String chave = "k" + (i % espacoDeChaves);
                    compartilhado.put(chave, i);
                    compartilhado.get(chave);
                }
                return null;
            }));
        }

        largada.countDown();
        executor.shutdown();
        boolean terminou = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertTrue(terminou, "as threads devem terminar dentro do tempo");

        // Propaga qualquer excecao (ex: ConcurrentModificationException) lancada
        // nas worker threads -- sem isto, a falha de concorrencia seria engolida.
        for (Future<?> f : futuros) {
            assertDoesNotThrow(() -> f.get(), "nenhuma thread deve lancar excecao de concorrencia");
        }
        assertTrue(compartilhado.size() <= capacidade, "cache nao deve exceder a capacidade");
    }
}
