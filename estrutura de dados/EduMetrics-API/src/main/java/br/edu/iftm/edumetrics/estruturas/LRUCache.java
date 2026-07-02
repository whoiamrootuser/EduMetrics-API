package br.edu.iftm.edumetrics.estruturas;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache LRU (Least Recently Used) baseado em {@link LinkedHashMap} com
 * {@code accessOrder=true}. Quando a capacidade maxima e atingida, o elemento
 * acessado ha mais tempo e removido automaticamente.
 *
 * <p><b>Complexidade:</b> {@code get} O(1) amortizado, {@code put} O(1)
 * amortizado, eviccao O(1). O LinkedHashMap mantem uma lista duplamente
 * encadeada que registra a ordem de acesso; cada {@code get}/{@code put} apenas
 * reposiciona um no nessa lista (operacao O(1)).</p>
 *
 * @param <K> tipo da chave -- deve ser imutavel (String, Long, record)
 * @param <V> tipo do valor
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacidade;

    /**
     * @param capacidade numero maximo de entradas antes de iniciar a eviccao
     */
    public LRUCache(int capacidade) {
        // accessOrder = true: cada get() move o no para o fim da lista de acesso
        // loadFactor = 0.75f: padrao do Java Collections Framework
        super(capacidade, 0.75f, /* accessOrder */ true);
        if (capacidade <= 0) {
            throw new IllegalArgumentException("capacidade deve ser positiva");
        }
        this.capacidade = capacidade;
    }

    /**
     * Invocado pelo {@link LinkedHashMap} apos cada {@code put()}.
     * Retorna {@code true} para remover a entrada mais antiga quando a
     * capacidade e excedida.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacidade;
    }

    /**
     * Fabrica thread-safe para uso em ambientes multi-thread (ex: requisicoes HTTP).
     * O {@code synchronizedMap} e necessario porque o reposicionamento causado por
     * {@code get()} em modo accessOrder e uma modificacao estrutural.
     */
    public static <K, V> Map<K, V> create(int capacidade) {
        return Collections.synchronizedMap(new LRUCache<>(capacidade));
    }

    public int getCapacidade() {
        return capacidade;
    }
}
