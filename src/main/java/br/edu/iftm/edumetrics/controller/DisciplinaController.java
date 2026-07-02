package br.edu.iftm.edumetrics.controller;

import br.edu.iftm.edumetrics.domain.dto.DisciplinaDTO;
import br.edu.iftm.edumetrics.service.AutocompletarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Endpoints REST de Disciplina, incluindo o autocompletar baseado na Trie.
 */
@RestController
@RequestMapping("/api/disciplinas")
@Tag(name = "Disciplinas", description = "Cadastro e autocompletar de disciplinas (Trie)")
public class DisciplinaController {

    private final AutocompletarService autocompletarService;

    public DisciplinaController(AutocompletarService autocompletarService) {
        this.autocompletarService = autocompletarService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar disciplina e indexa-la na Trie")
    public ResponseEntity<DisciplinaDTO> criar(@Valid @RequestBody DisciplinaDTO dto) {
        DisciplinaDTO salva = autocompletarService.criarDisciplina(dto);
        return ResponseEntity.created(URI.create("/api/disciplinas/" + salva.id())).body(salva);
    }

    @GetMapping("/autocompletar")
    @Operation(summary = "Autocompletar nomes de disciplina por prefixo -- Trie O(|prefixo|)")
    public ResponseEntity<List<String>> autocompletar(@RequestParam("q") String q) {
        return ResponseEntity.ok(autocompletarService.sugerir(q));
    }
}
