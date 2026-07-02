package br.edu.iftm.edumetrics.service;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.repository.AlunoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Teste de integracao do caminho REAL do Spring Cache (@Cacheable/@CacheEvict),
 * exercitado pelo proxy do Spring. Usa o cache em memoria do perfil de teste
 * (nao requer Redis) e um {@code @SpyBean} no repositorio para contar quantas
 * vezes o banco foi de fato consultado.
 */
@SpringBootTest
class CacheIntegrationTest {

    @SpyBean
    private AlunoRepository alunoRepository;

    @Autowired
    private AlunoService alunoService;

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
        clearInvocations(alunoRepository);   // zera a contagem feita durante o setup
        return qualquer.getId();
    }

    @Test
    @DisplayName("@Cacheable: a 2a chamada a buscarPorId NAO consulta o banco")
    void cacheableEvitaSegundaConsultaAoBanco() {
        Long id = idDeUmAlunoSemeado();

        assertNotNull(alunoService.buscarPorId(id));   // MISS -> banco
        assertNotNull(alunoService.buscarPorId(id));   // HIT  -> cache (sem banco)

        verify(alunoRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("@CacheEvict: limparCache forca a proxima busca a consultar o banco de novo")
    void cacheEvictForcaNovaConsulta() {
        Long id = idDeUmAlunoSemeado();

        alunoService.buscarPorId(id);     // MISS -> banco (1)
        alunoService.buscarPorId(id);     // HIT  -> cache
        alunoService.limparCache();       // @CacheEvict(value="alunos", allEntries=true)
        alunoService.buscarPorId(id);     // MISS de novo -> banco (2)

        verify(alunoRepository, times(2)).findById(id);
    }
}
