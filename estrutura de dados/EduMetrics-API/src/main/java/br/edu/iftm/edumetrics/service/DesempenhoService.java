package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.domain.Desempenho;
import br.edu.iftm.edumetrics.domain.Disciplina;
import br.edu.iftm.edumetrics.domain.dto.DesempenhoDTO;
import br.edu.iftm.edumetrics.domain.dto.RegistroDesempenhoDTO;
import br.edu.iftm.edumetrics.exception.AlunoNaoEncontradoException;
import br.edu.iftm.edumetrics.exception.DisciplinaNaoEncontradaException;
import br.edu.iftm.edumetrics.repository.AlunoRepository;
import br.edu.iftm.edumetrics.repository.DesempenhoRepository;
import br.edu.iftm.edumetrics.repository.DisciplinaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servico de Desempenho: registra notas e lista o desempenho de um aluno.
 */
@Service
public class DesempenhoService {

    private final DesempenhoRepository desempenhoRepo;
    private final AlunoRepository alunoRepo;
    private final DisciplinaRepository disciplinaRepo;
    private final RankingService rankingService;

    public DesempenhoService(DesempenhoRepository desempenhoRepo,
                             AlunoRepository alunoRepo,
                             DisciplinaRepository disciplinaRepo,
                             RankingService rankingService) {
        this.desempenhoRepo = desempenhoRepo;
        this.alunoRepo = alunoRepo;
        this.disciplinaRepo = disciplinaRepo;
        this.rankingService = rankingService;
    }

    /**
     * Registra uma nota. A nota final e calculada como (nota1 + nota2) / 2.
     * Invalida o cache de desempenhos do aluno e o cache de ranking.
     */
    @CacheEvict(value = "desempenhos", key = "#req.alunoId()")
    @Transactional
    public DesempenhoDTO registrar(RegistroDesempenhoDTO req) {
        Aluno aluno = alunoRepo.findById(req.alunoId())
                .orElseThrow(() -> new AlunoNaoEncontradoException(req.alunoId()));
        Disciplina disciplina = disciplinaRepo.findById(req.disciplinaId())
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(req.disciplinaId()));

        Desempenho desempenho = new Desempenho(aluno, disciplina, req.nota1(), req.nota2(), req.semestre());
        Desempenho salvo = desempenhoRepo.save(desempenho);

        rankingService.invalidarRanking();   // o ranking mudou
        return toDTO(salvo);
    }

    /**
     * Lista o desempenho de um aluno. Usa JOIN FETCH (1 query, sem N+1) e
     * armazena o resultado no cache "desempenhos" (TTL de 1 hora -- ver CacheConfig).
     */
    @Cacheable(value = "desempenhos", key = "#alunoId")
    @Transactional(readOnly = true)
    public List<DesempenhoDTO> listarPorAluno(Long alunoId) {
        if (!alunoRepo.existsById(alunoId)) {
            throw new AlunoNaoEncontradoException(alunoId);
        }
        return desempenhoRepo.findByAlunoIdFetchDisciplina(alunoId).stream()
                .map(this::toDTO)
                .toList();
    }

    private DesempenhoDTO toDTO(Desempenho d) {
        return new DesempenhoDTO(
                d.getDisciplina().getNome(),
                d.getNota1(),
                d.getNota2(),
                d.getNotaFinal(),
                d.getSemestre());
    }
}
