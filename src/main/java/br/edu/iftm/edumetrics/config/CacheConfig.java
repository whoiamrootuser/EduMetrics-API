package br.edu.iftm.edumetrics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configuracao do cache Redis com TTL.
 *
 * <p>O bean so e criado quando {@code spring.cache.type=redis} (perfil padrao).
 * No perfil de teste ({@code spring.cache.type=simple}) o Spring Boot fornece um
 * cache em memoria, evitando a dependencia de um Redis em execucao.</p>
 */
@Configuration
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:300000}")
    private long ttlMs;

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Serializer JSON com informacao de tipo (@class) -- preserva a classe
        // concreta dos records ao gravar/ler do Redis.
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(ttlMs))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // Cache de desempenho com TTL diferenciado (dados mudam menos)
                .withCacheConfiguration("desempenhos", config.entryTtl(Duration.ofHours(1)))
                .build();
    }
}
