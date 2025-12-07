package be.esi.rencontres.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// Spring Boot détecte cette classe au démarrage et l'utilise pour paramétrer l'application
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
       
        // Permet d'avoir des clés lisibles (ex: "user:123") au lieu de binaire
        template.setKeySerializer(new StringRedisSerializer());
        
        // Pour que les VALEURS soient du texte et pas du binaire
        // Sans ça, PointsService va planter en essayant de lire le score
        template.setValueSerializer(new StringRedisSerializer());

        // objet Java "prêt à l'emploi" qui te donne des boutons simples (des méthodes) 
        // pour interagir avec la base de données.
        return template;
    }
}
