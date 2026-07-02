package br.edu.iftm.edumetrics.security;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao HTTP do filtro de rate limit.
 *
 * <p>Usa um limite reduzido apenas nesta classe para validar rapidamente que o
 * filtro devolve 429 na (N+1)-esima requisicao do mesmo cliente.</p>
 */
@SpringBootTest(properties = {
        "rate.limiter.max-requisicoes=3",
        "rate.limiter.janela-ms=60000"
})
@AutoConfigureMockMvc
class RateLimitFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve retornar 429 na 4a requisicao do mesmo IP dentro da janela")
    void deveRetornar429AposExcederLimite() throws Exception {
        String cliente = "198.51.100.77";

        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            mockMvc.perform(get("/api/disciplinas/autocompletar")
                            .param("q", "Alg")
                            .header("X-Forwarded-For", cliente))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/disciplinas/autocompletar")
                        .param("q", "Alg")
                        .header("X-Forwarded-For", cliente))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(containsString("Rate limit excedido")))
                .andExpect(content().string(containsString("\"limite\": 3")));
    }
}