package br.edu.iftm.edumetrics.controller;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;

/**
 * Valida o contrato sem N+1 no endpoint de desempenho por aluno.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class DesempenhoControllerQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CacheManager cacheManager;

    private Statistics statistics;

    @BeforeEach
    void prepararCenario() {
        cacheManager.getCacheNames().forEach(nome -> {
            var cache = cacheManager.getCache(nome);
            if (cache != null) {
                cache.clear();
            }
        });

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("GET /api/alunos/{id}/desempenho executa no maximo 2 queries")
    void desempenhoPorAlunoNaoSofreNMaisUm() throws Exception {
        mockMvc.perform(get("/api/alunos/{id}/desempenho", 1L))
                .andExpect(status().isOk());

        long queriesExecutadas = statistics.getPrepareStatementCount();
        assertTrue(queriesExecutadas <= 2,
                "esperava no maximo 2 queries, mas houve " + queriesExecutadas);
    }
}