package be.esi.rencontres.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Configuration pour Elasticsearch
 * Utilise l'auto-configuration de Spring Boot via application.properties
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "be.esi.rencontres.user.repository")
public class ElasticsearchConfig {
    // Spring Boot auto-configure Elasticsearch via application.properties
    // Pas besoin de configuration manuelle avec Spring Boot 3.x
}
