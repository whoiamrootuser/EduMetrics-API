package br.edu.iftm.edumetrics.benchmark;

import br.edu.iftm.edumetrics.domain.dto.AlunoDTO;
import br.edu.iftm.edumetrics.domain.dto.RankingItemDTO;
import br.edu.iftm.edumetrics.estruturas.LRUCache;
import br.edu.iftm.edumetrics.estruturas.RateLimiter;
import br.edu.iftm.edumetrics.estruturas.Trie;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Suite JMH do EduMetrics.
 *
 * <p>Compara as estruturas customizadas contra alternativas ingenuas:
 * LRUCache vs HashMap direto, Trie vs varredura linear, e mede o custo do
 * RateLimiter (Sliding Window).</p>
 *
 * <p>Execucao via uber-jar (profile benchmark):</p>
 * <pre>
 *   mvn clean package -Pbenchmark -DskipTests
 *   java -jar target/benchmarks.jar
 *   java -jar target/benchmarks.jar -rf json -rff jmh-resultado.json
 * </pre>
 *
 * <p>Tambem pode ser executada pela IDE chamando o metodo {@link #main(String[])}.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx1g", "-XX:+UseG1GC"})
public class EduMetricsBenchmark {

    private static final int N = 50_000;

    private Map<String, AlunoDTO> lruCache;
    private HashMap<String, AlunoDTO> hashMapIndice;
    private Trie trie;
    private RateLimiter rateLimiter;
    private PriorityQueue<RankingItemDTO> minHeap;
    private List<String> matriculas;
    private List<String> prefixos;

    @Setup(Level.Trial)
    public void setup() {
        lruCache = LRUCache.create(1000);
        hashMapIndice = new HashMap<>(N);
        trie = new Trie();
        rateLimiter = new RateLimiter(100, 60_000L);
        minHeap = new PriorityQueue<>(Comparator.comparingDouble(RankingItemDTO::mediaGeral));
        matriculas = new ArrayList<>(N);
        prefixos = List.of("Alg", "Est", "Pro", "Mat", "Ban");

        Random rand = new Random(42);
        String[] disciplinas = {
                "Algoritmos", "Estrutura de Dados", "Programacao Web",
                "Matematica Discreta", "Banco de Dados", "Sistemas Operacionais"
        };

        for (int i = 0; i < N; i++) {
            String mat = String.format("2024%05d", i);
            AlunoDTO dto = new AlunoDTO((long) i, mat, "Aluno " + i,
                    "aluno" + i + "@iftm.edu.br", "TSI", 5);
            matriculas.add(mat);
            hashMapIndice.put(mat, dto);
            lruCache.put(mat, dto);

            RankingItemDTO ranking = new RankingItemDTO(i + 1, "Aluno " + i, mat,
                    5.0 + rand.nextDouble() * 5.0, rand.nextInt(10) + 1);
            minHeap.offer(ranking);
            if (minHeap.size() > 10) {
                minHeap.poll();   // manter top-10
            }
        }
        for (String d : disciplinas) {
            trie.inserir(d);
        }
    }

    // ── Benchmark 1: LRU Cache vs HashMap direto ──────────────────────────
    @Benchmark
    public AlunoDTO buscaLRUCache(Blackhole bh) {
        String mat = matriculas.get(ThreadLocalRandom.current().nextInt(N));
        AlunoDTO dto = lruCache.get(mat);
        bh.consume(dto);
        return dto;
    }

    @Benchmark
    public AlunoDTO buscaHashMapDireto(Blackhole bh) {
        String mat = matriculas.get(ThreadLocalRandom.current().nextInt(N));
        AlunoDTO dto = hashMapIndice.get(mat);
        bh.consume(dto);
        return dto;
    }

    // ── Benchmark 2: Trie vs varredura linear ─────────────────────────────
    @Benchmark
    public List<String> autocompletarTrie(Blackhole bh) {
        String pref = prefixos.get(ThreadLocalRandom.current().nextInt(prefixos.size()));
        List<String> r = trie.autocompletar(pref, 10);
        bh.consume(r);
        return r;
    }

    @Benchmark
    public List<String> autocompletarLinear(Blackhole bh) {
        String pref = prefixos.get(ThreadLocalRandom.current().nextInt(prefixos.size())).toLowerCase();
        List<String> r = hashMapIndice.keySet().stream()
                .filter(k -> k.startsWith(pref))
                .limit(10)
                .toList();
        bh.consume(r);
        return r;
    }

    // ── Benchmark 3: Rate Limiter (Sliding Window) ────────────────────────
    @Benchmark
    public boolean rateLimiterCheck(Blackhole bh) {
        // Simula 10 clientes distintos para nao saturar um unico
        String clienteId = "cliente-" + ThreadLocalRandom.current().nextInt(10);
        boolean ok = rateLimiter.permitir(clienteId);
        bh.consume(ok);
        return ok;
    }

    /** Permite rodar a suite pela IDE (Run) sem o uber-jar. */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(EduMetricsBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
