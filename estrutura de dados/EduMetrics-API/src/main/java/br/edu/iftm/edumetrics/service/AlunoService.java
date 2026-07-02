package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.domain.dto.AlunoDTO;
import br.edu.iftm.edumetrics.estruturas.LRUCache;
import br.edu.iftm.edumetrics.exception.AlunoNaoEncontradoException;
import br.edu.iftm.edumetrics.exception.ConflitoException;
import br.edu.iftm.edumetrics.repository.AlunoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servico de Aluno.
 *
 * <p>Duas camadas de cache:</p>
 * <ul>
 *   <li><b>LRUCache local</b> (por matricula) -- O(1), em memoria, com contadores
 *       de hit/miss expostos em /api/admin/cache/stats.</li>
 *   <li><b>Spring Cache + Redis</b> (por id) -- via {@code @Cacheable},
 *       {@code @CachePut} e {@code @CacheEvict}.</li>
 * </ul>
 */
@Service
public class AlunoService {

    private static final Logger log = LoggerFactory.getLogger(AlunoService.class);
    private static final int CACHE_CAPACIDADE = 500;

    private final AlunoRepository repository;
    private final AutocompletarService autocompletar;

    // Mantemos a instancia concreta do LRUCache (para expor capacidade nas stats)
    // e uma visao sincronizada dela para uso concorrente seguro.
    private final LRUCache<String, AlunoDTO> lru = new LRUCache<>(CACHE_CAPACIDADE);
    private final Map<String, AlunoDTO> cacheMatricula = Collections.synchronizedMap(lru);

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public AlunoService(AlunoRepository repository, AutocompletarService autocompletar) {
        this.repository = repository;
        this.autocompletar = autocompletar;
    }

    /**
     * Busca por id com cache Redis. Em cache HIT o metodo nem chega a executar
     * (interceptado pelo proxy do Spring Cache); em MISS, consulta o banco.
     */
    @Cacheable(value = "alunos", key = "#id")
    public AlunoDTO buscarPorId(Long id) {
        log.debug("Cache MISS (alunos) -> consultando BD para id={}", id);
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new AlunoNaoEncontradoException(id));
    }

    /**
     * Busca por matricula usando o LRUCache local -- O(1). Em cache miss,
     * consulta o banco e popula o cache.
     */
    public AlunoDTO buscarPorMatricula(String matricula) {
        AlunoDTO dto = cacheMatricula.get(matricula);
        if (dto != null) {
            hits.incrementAndGet();
            return dto;
        }
        misses.incrementAndGet();
        Aluno aluno = repository.findByMatricula(matricula)
                .orElseThrow(() -> new AlunoNaoEncontradoException(matricula));
        dto = toDTO(aluno);
        cacheMatricula.put(matricula, dto);
        return dto;
    }

    /**
     * Cadastra um aluno. Atualiza o cache Redis ({@code @CachePut}) e a Trie
     * (autocompletar), alem de popular o cache local por matricula.
     */
    @CachePut(value = "alunos", key = "#result.id()")
    public AlunoDTO salvar(AlunoDTO dto) {
        if (repository.existsByMatricula(dto.matricula())) {
            throw new ConflitoException("Matricula ja cadastrada: " + dto.matricula());
        }
        if (repository.existsByEmail(dto.email())) {
            throw new ConflitoException("Email ja cadastrado: " + dto.email());
        }
        Aluno aluno = new Aluno(dto.matricula(), dto.nome(), dto.email(), dto.curso(), dto.periodo());
        Aluno salvo = repository.save(aluno);
        autocompletar.indexar(salvo.getNome());   // mantem a Trie atualizada
        AlunoDTO resultado = toDTO(salvo);
        cacheMatricula.put(salvo.getMatricula(), resultado);
        return resultado;
    }

    /**
     * Atualiza um aluno existente: grava no banco e no cache Redis simultaneamente.
     */
    @CachePut(value = "alunos", key = "#id")
    public AlunoDTO atualizar(Long id, AlunoDTO dto) {
        Aluno aluno = repository.findById(id)
                .orElseThrow(() -> new AlunoNaoEncontradoException(id));

        if (dto.matricula() != null && !dto.matricula().equals(aluno.getMatricula())) {
            cacheMatricula.remove(aluno.getMatricula());   // invalida a chave antiga local
            aluno.setMatricula(dto.matricula());
        }
        aluno.setNome(dto.nome());
        aluno.setEmail(dto.email());
        aluno.setCurso(dto.curso());
        aluno.setPeriodo(dto.periodo());

        Aluno salvo = repository.save(aluno);
        autocompletar.indexar(salvo.getNome());
        AlunoDTO resultado = toDTO(salvo);
        cacheMatricula.put(salvo.getMatricula(), resultado);
        return resultado;
    }

    /** Remove do banco e invalida o cache Redis e o cache local. */
    @CacheEvict(value = "alunos", key = "#id")
    public void remover(Long id) {
        Aluno aluno = repository.findById(id)
                .orElseThrow(() -> new AlunoNaoEncontradoException(id));
        cacheMatricula.remove(aluno.getMatricula());
        repository.deleteById(id);
    }

    /** Limpa todo o cache Redis de alunos (usado pelo endpoint /api/admin/cache). */
    @CacheEvict(value = "alunos", allEntries = true)
    public void limparCache() {
        cacheMatricula.clear();
        log.info("Cache de alunos (Redis + local) limpo");
    }

    /** Estatisticas do LRUCache local para o endpoint /api/admin/cache/stats. */
    public Map<String, Object> cacheStats() {
        long h = hits.get();
        long m = misses.get();
        long total = h + m;
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("entradas", cacheMatricula.size());
        stats.put("capacidade", lru.getCapacidade());
        stats.put("hits", h);
        stats.put("misses", m);
        stats.put("hitRate", total == 0 ? 0.0 : (double) h / total);
        return stats;
    }

    private AlunoDTO toDTO(Aluno a) {
        return new AlunoDTO(a.getId(), a.getMatricula(), a.getNome(),
                a.getEmail(), a.getCurso(), a.getPeriodo());
    }
}
