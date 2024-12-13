package com.jeka8833.tntserver.configs;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000));

        caffeineCacheManager.registerCustomCache("mojang",
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .build());

        return caffeineCacheManager;
    }
}
