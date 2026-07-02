package br.edu.iftm.edumetrics.controller;

import br.edu.iftm.edumetrics.domain.dto.DesempenhoDTO;
import br.edu.iftm.edumetrics.domain.dto.RankingItemDTO;
import br.edu.iftm.edumetrics.domain.dto.RegistroDesempenhoDTO;
import br.edu.iftm.edumetrics.service.DesempenhoService;
import br.edu.iftm.edumetrics.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de notas, desempenho por aluno e ranking.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Desempenho e Ranking", description = "Notas, medias e ranking Top-K (PriorityQueue)")
public class DesempenhoController {

    private final DesempenhoService desempenhoService;
    private final RankingService rankingService;

    public DesempenhoController(DesempenhoService desempenhoService, RankingService rankingService) {
        this.desempenhoService = desempenhoService;
        this.rankingService = rankingService;
    }

    @PostMapping("/desempenhos")
    @Operation(summary = "Registrar nota -- invalida o cache de ranking (@CacheEvict)")
    public ResponseEntity<DesempenhoDTO> registrar(@Valid @RequestBody RegistroDesempenhoDTO req) {
        DesempenhoDTO dto = desempenhoService.registrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);   // 201
    }

    @GetMapping("/alunos/{id}/desempenho")
    @Operation(summary = "Desempenho do aluno -- @Cacheable(\"desempenhos\"), JOIN FETCH (sem N+1)")
    public ResponseEntity<List<DesempenhoDTO>> porAluno(@PathVariable Long id) {
        return ResponseEntity.ok(desempenhoService.listarPorAluno(id));
    }

    @GetMapping("/ranking")
    @Operation(summary = "Top-K alunos por media -- min-heap O(n log k), @Cacheable(\"ranking\")")
    public ResponseEntity<List<RankingItemDTO>> ranking(
            @RequestParam(name = "top", defaultValue = "10") int top) {
        return ResponseEntity.ok(rankingService.topK(top));
    }
}
