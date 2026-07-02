package br.edu.iftm.edumetrics.controller;

import br.edu.iftm.edumetrics.domain.dto.AlunoDTO;
import br.edu.iftm.edumetrics.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints REST de Aluno (CRUD).
 */
@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos", description = "Cadastro e consulta de alunos (cache Redis + LRUCache local)")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar aluno (valida matricula unica e indexa o nome na Trie)")
    public ResponseEntity<AlunoDTO> cadastrar(@Valid @RequestBody AlunoDTO dto) {
        AlunoDTO salvo = alunoService.salvar(dto);
        URI location = URI.create("/api/alunos/" + salvo.id());
        return ResponseEntity.created(location).body(salvo);   // 201
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar por id -- @Cacheable(\"alunos\"): 2o acesso vem do Redis")
    public ResponseEntity<AlunoDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(alunoService.buscarPorId(id));   // 200
    }

    @GetMapping("/matricula/{matricula}")
    @Operation(summary = "Buscar por matricula -- LRUCache local O(1); miss consulta o BD")
    public ResponseEntity<AlunoDTO> buscarPorMatricula(@PathVariable String matricula) {
        return ResponseEntity.ok(alunoService.buscarPorMatricula(matricula));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar -- @CachePut: grava no BD e no cache simultaneamente")
    public ResponseEntity<AlunoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody AlunoDTO dto) {
        return ResponseEntity.ok(alunoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover -- @CacheEvict: remove do BD e invalida o cache")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        alunoService.remover(id);
        return ResponseEntity.noContent().build();   // 204
    }
}
