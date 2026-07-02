package br.edu.iftm.edumetrics.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.repository.AlunoRepository;

/**
 * Testes HTTP do caminho real de cache dos endpoints de aluno.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlunoControllerCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private AlunoRepository alunoRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void limparCaches() {
        cacheManager.getCacheNames().forEach(nome -> {
            var cache = cacheManager.getCache(nome);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    private Long idDeUmAlunoSemeado() {
        Aluno qualquer = alunoRepository.findAll().get(0);
        clearInvocations(alunoRepository);
        return qualquer.getId();
    }

    @Test
    @DisplayName("GET /api/alunos/{id}: a 2a chamada nao consulta o banco novamente")
    void getPorIdUsaCacheNaSegundaChamada() throws Exception {
        Long id = idDeUmAlunoSemeado();

        mockMvc.perform(get("/api/alunos/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/alunos/{id}", id))
                .andExpect(status().isOk());

        verify(alunoRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("DELETE /api/alunos/{id}: invalida o cache para a proxima busca")
    void deleteInvalidaCache() throws Exception {
        Long id = idDeUmAlunoSemeado();

        mockMvc.perform(get("/api/alunos/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/alunos/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/alunos/{id}", id))
                .andExpect(status().isNotFound());

        verify(alunoRepository, times(3)).findById(id);
    }
}