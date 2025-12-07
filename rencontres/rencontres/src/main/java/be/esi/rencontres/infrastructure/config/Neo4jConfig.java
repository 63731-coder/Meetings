package be.esi.rencontres.infrastructure.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    // Connexion basée sur le docker-compose.yml (neo4j/password sur le port 7687)
    private static final String URI = "bolt://localhost:7687";
    private static final String USER = "neo4j";
    private static final String PASSWORD = "password";

    @Bean // Expose l'objet Driver à Spring pour qu'il puisse être injecté partout
    public Driver neo4jDriver() {
        return GraphDatabase.driver(URI, AuthTokens.basic(USER, PASSWORD));
    }
}