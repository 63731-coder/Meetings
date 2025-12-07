package be.esi.rencontres.points.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PointsService {

    // On déclare notre télécommande Redis
    private final RedisTemplate<String, Object> redisTemplate;

    // Spring va injecter la télécommande Redis toute prête ici
    public PointsService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Si userId vaut "u42", la clé devient "score:u42"
    public void addPoints(String userId, int points){
        String key = "score:" + userId;

        // On utilise la télécommande pour ajouter des points
        // Si la clé "score:u42" n'existe pas, Redis la crée et met la valeur à 0
        redisTemplate.opsForValue().increment(key, points);
    }

    public Integer getPoints(String userId){
        String key = "score:" + userId;
        Object value = redisTemplate.opsForValue().get(key);
        
        if(value != null){
            // On convertit le texte "10" en entier 10
            return Integer.parseInt((String) value);
        } else {
            return 0;
        }
    }
}