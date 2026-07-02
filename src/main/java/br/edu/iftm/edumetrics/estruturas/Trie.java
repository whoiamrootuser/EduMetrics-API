package br.edu.iftm.edumetrics.estruturas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arvore de prefixos (Trie) para autocompletar nomes de disciplinas.
 *
 * <p>Cada no representa um caractere; caminhos da raiz ate nos terminais formam
 * as palavras inseridas.</p>
 *
 * <p><b>Complexidade:</b> inserir {@code O(|palavra|)};
 * autocompletar {@code O(|prefixo| + k)} -- a descida ate o prefixo independe do
 * total de palavras armazenadas (diferente da varredura linear O(n)).</p>
 */
public class Trie {

    private static final class No {
        final Map<Character, No> filhos = new HashMap<>();
        boolean fimDePalavra = false;
        String valorCompleto;   // armazena o termo completo (com formatacao original)
    }

    private final No raiz = new No();
    private int totalPalavras = 0;

    /**
     * Insere uma palavra na Trie. Complexidade: {@code O(|palavra|)}.
     * Palavras nulas ou em branco sao ignoradas; duplicatas nao sao recontadas.
     */
    public void inserir(String palavra) {
        if (palavra == null || palavra.isBlank()) {
            return;
        }
        No atual = raiz;
        // Indexamos em minusculo para busca case-insensitive
        for (char c : palavra.toLowerCase().toCharArray()) {
            atual = atual.filhos.computeIfAbsent(c, k -> new No());
        }
        if (!atual.fimDePalavra) {
            atual.fimDePalavra = true;
            atual.valorCompleto = palavra;   // preserva a formatacao original
            totalPalavras++;
        }
    }

    /**
     * Retorna ate {@code maxResultados} termos que comecam com o prefixo.
     * Complexidade: {@code O(|prefixo|)} para a descida + {@code O(k)} para a coleta.
     *
     * @param prefixo       string de busca (case-insensitive)
     * @param maxResultados limite superior de resultados
     * @return lista imutavel de termos correspondentes, em ordem de DFS
     */
    public List<String> autocompletar(String prefixo, int maxResultados) {
        if (prefixo == null || prefixo.isBlank() || maxResultados <= 0) {
            return List.of();
        }
        No no = descerAte(prefixo.toLowerCase());
        if (no == null) {
            return List.of();
        }
        List<String> resultados = new ArrayList<>();
        coletarPalavras(no, resultados, maxResultados);
        return Collections.unmodifiableList(resultados);
    }

    private No descerAte(String prefixo) {
        No atual = raiz;
        for (char c : prefixo.toCharArray()) {
            atual = atual.filhos.get(c);
            if (atual == null) {
                return null;
            }
        }
        return atual;
    }

    private void coletarPalavras(No no, List<String> resultado, int max) {
        if (resultado.size() >= max) {
            return;
        }
        if (no.fimDePalavra) {
            resultado.add(no.valorCompleto);
        }
        for (No filho : no.filhos.values()) {
            if (resultado.size() >= max) {
                return;
            }
            coletarPalavras(filho, resultado, max);
        }
    }

    public int size() {
        return totalPalavras;
    }

    public boolean isEmpty() {
        return totalPalavras == 0;
    }
}
