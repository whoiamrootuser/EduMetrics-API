package br.edu.iftm.edumetrics.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste HTTP do endpoint assincrono de relatorios.
 */
@SpringBootTest(properties = "edumetrics.mensageria.habilitada=false")
@AutoConfigureMockMvc
class RelatorioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/relatorios retorna 202 em menos de 100 ms")
    void postRelatoriosRetorna202Rapidamente() throws Exception {
        String body = """
                {
                  \"alunoId\": 1,
                  \"tipo\": \"BOLETIM\",
                  \"semestre\": \"2026/1\"
                }
                """;

        long inicio = System.nanoTime();

        mockMvc.perform(post("/api/relatorios")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("correlationId")));

        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(duracaoMs < 100, "POST /api/relatorios deveria responder em menos de 100 ms, mas levou " + duracaoMs + " ms");
    }
}