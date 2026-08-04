package com.bachratus.demo.config;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class NoHibernateCacheTestConfiguration {

    @Bean
    HibernatePropertiesCustomizer disableHibernateCacheCustomizer() {
        return properties -> {
            properties.put("hibernate.cache.use_second_level_cache", false);
            properties.put("hibernate.cache.use_query_cache", false);
            properties.put("hibernate.cache.region.factory_class", NoCachingRegionFactory.class.getName());
        };
    }
}
