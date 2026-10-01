package com.inventory.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    public static final String CACHE_DASHBOARD_STATS = "dashboardStats";
    public static final String CACHE_PRODUCTS_LIST = "productsList";
    public static final String CACHE_PRODUCT_DETAILS = "productDetails";
    public static final String CACHE_LOW_STOCK_PRODUCTS = "lowStockProducts";
    public static final String CACHE_CATEGORIES_LIST = "categoriesList";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_DASHBOARD_STATS, defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CACHE_PRODUCTS_LIST, defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put(CACHE_PRODUCT_DETAILS, defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CACHE_LOW_STOCK_PRODUCTS, defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CACHE_CATEGORIES_LIST, defaultCacheConfig.entryTtl(Duration.ofMinutes(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis unreachable during cache GET on cache '{}' with key '{}'. Falling back to PostgreSQL database. Error: {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis unreachable during cache PUT on cache '{}' with key '{}'. Error: {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis unreachable during cache EVICT on cache '{}' with key '{}'. Error: {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis unreachable during cache CLEAR on cache '{}'. Error: {}",
                        cache != null ? cache.getName() : "unknown", exception.getMessage());
            }
        };
    }
}
