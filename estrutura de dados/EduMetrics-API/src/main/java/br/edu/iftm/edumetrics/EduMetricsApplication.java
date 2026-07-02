package br.edu.iftm.edumetrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Ponto de entrada da aplicacao EduMetrics API.
 *
 * <p>Sistema RESTful de acompanhamento de desempenho academico que integra
 * todas as estruturas de dados do semestre: LRUCache, Trie e RateLimiter
 * (estruturas customizadas), alem de PriorityQueue, HashMap, Redis e RabbitMQ.</p>
 */
@SpringBootApplication
@EnableCaching
public class EduMetricsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduMetricsApplication.class, args);
    }
}
