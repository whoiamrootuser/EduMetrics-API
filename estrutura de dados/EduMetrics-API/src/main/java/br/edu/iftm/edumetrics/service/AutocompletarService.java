package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Disciplina;
import br.edu.iftm.edumetrics.domain.dto.DisciplinaDTO;
import br.edu.iftm.edumetrics.estruturas.Trie;
import br.edu.iftm.edumetrics.exception.ConflitoException;
import br.edu.iftm.edumetrics.repository.DisciplinaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servico de autocompletar baseado na {@link Trie}.
 *
 * <p>A Trie e carregada no startup (@PostConstruct). O {@code @DependsOn} garante
 * que o {@code DataLoader} ja semeou as disciplinas no banco antes de a Trie ser
 * populada.</p>
 */
@Service
@DependsOn("dataLoader")
public class AutocompletarService {

    private static final Logger log = LoggerFactory.getLogger(AutocompletarService.class);
    private static final int MAX_SUGESTOES = 10;

    private final Trie trie = new Trie();
    private final DisciplinaRepository disciplinaRepo;

    public AutocompletarService(DisciplinaRepository disciplinaRepo) {
        this.disciplinaRepo = disciplinaRepo;
    }

    /** Executa uma vez no startup da aplicacao: carrega a Trie com as disciplinas. */
    @PostConstruct
    public void carregarDisciplinas() {
        disciplinaRepo.findAll().forEach(d -> trie.inserir(d.getNome()));
        log.info("Trie carregada com {} termos (disciplinas)", trie.size());
    }

    /** Autocompletar O(|prefixo| + k). */
    public List<String> sugerir(String prefixo) {
        return trie.autocompletar(prefixo, MAX_SUGESTOES);
    }

    /** Indexa um novo termo na Trie (chamado ao cadastrar disciplina ou aluno). */
    public void indexar(String termo) {
        trie.inserir(termo);
    }

    /** Total de termos indexados (exposto no /api/admin/health). */
    public int totalIndexado() {
        return trie.size();
    }

    /** Cadastra uma nova disciplina, persiste e indexa o nome na Trie. */
    @Transactional
    public DisciplinaDTO criarDisciplina(DisciplinaDTO dto) {
        if (disciplinaRepo.existsByCodigo(dto.codigo())) {
            throw new ConflitoException("Codigo de disciplina ja cadastrado: " + dto.codigo());
        }
        Disciplina nova = new Disciplina(dto.codigo(), dto.nome(), dto.creditos());
        Disciplina salva = disciplinaRepo.save(nova);
        indexar(salva.getNome());
        log.info("Disciplina cadastrada e indexada na Trie: {}", salva.getNome());
        return new DisciplinaDTO(salva.getId(), salva.getCodigo(), salva.getNome(), salva.getCreditos());
    }
}
