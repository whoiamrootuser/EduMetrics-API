package br.edu.iftm.edumetrics.estruturas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieTest {

    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
        trie.inserir("Algoritmos");
        trie.inserir("Algebra Linear");
        trie.inserir("Estrutura de Dados");
        trie.inserir("Banco de Dados");
    }

    @Test
    @DisplayName("Deve inserir e recuperar por prefixo exato")
    void deveRecuperarPorPrefixo() {
        List<String> r = trie.autocompletar("Alg", 10);
        assertEquals(2, r.size());
        assertTrue(r.contains("Algoritmos"));
        assertTrue(r.contains("Algebra Linear"));
    }

    @Test
    @DisplayName("Prefixo inexistente retorna lista vazia")
    void prefixoInexistenteRetornaVazio() {
        assertTrue(trie.autocompletar("xyz", 10).isEmpty());
    }

    @Test
    @DisplayName("Deve respeitar o limite maxResultados")
    void deveRespeitarLimite() {
        trie.inserir("Algumas Coisas");
        List<String> r = trie.autocompletar("Alg", 1);
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Busca deve ser case-insensitive")
    void deveSerCaseInsensitive() {
        assertEquals(2, trie.autocompletar("alg", 10).size());
        assertEquals(2, trie.autocompletar("ALG", 10).size());
    }

    @Test
    @DisplayName("Palavra duplicada nao duplica no resultado nem no tamanho")
    void palavraDuplicadaNaoDuplica() {
        int antes = trie.size();
        trie.inserir("Algoritmos");   // ja existe
        assertEquals(antes, trie.size());
        assertEquals(2, trie.autocompletar("Alg", 10).size());
    }

    @Test
    @DisplayName("Prefixo vazio ou nulo retorna lista vazia")
    void prefixoVazioRetornaVazio() {
        assertTrue(trie.autocompletar("", 10).isEmpty());
        assertTrue(trie.autocompletar(null, 10).isEmpty());
        assertTrue(trie.autocompletar("  ", 10).isEmpty());
    }

    @Test
    @DisplayName("Insercao nula ou em branco e ignorada")
    void insercaoInvalidaIgnorada() {
        int antes = trie.size();
        trie.inserir(null);
        trie.inserir("   ");
        assertEquals(antes, trie.size());
    }

    @Test
    @DisplayName("size() e isEmpty() refletem o estado da Trie")
    void sizeEIsEmpty() {
        Trie vazia = new Trie();
        assertTrue(vazia.isEmpty());
        assertEquals(0, vazia.size());
        vazia.inserir("Teste");
        assertFalse(vazia.isEmpty());
        assertEquals(1, vazia.size());
    }

    @Test
    @DisplayName("Preserva a formatacao original (maiusculas) do termo inserido")
    void preservaFormatacaoOriginal() {
        List<String> r = trie.autocompletar("est", 10);
        assertEquals(1, r.size());
        assertEquals("Estrutura de Dados", r.get(0));
    }

    @Test
    @DisplayName("Respeita o limite parando no meio da descida (no terminal com filhos)")
    void respeitaLimiteEmNoTerminalComFilhos() {
        Trie t = new Trie();
        t.inserir("a");     // no terminal que tambem tem filhos
        t.inserir("ab");
        t.inserir("abc");
        List<String> r = t.autocompletar("a", 2);
        assertEquals(2, r.size());   // coleta "a" e "ab" e para antes de "abc"
        assertTrue(r.contains("a"));
        assertTrue(r.contains("ab"));
    }
}
