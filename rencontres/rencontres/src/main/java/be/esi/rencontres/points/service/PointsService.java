package be.esi.rencontres.points.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PointsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LEADERBOARD_KEY = "leaderboard";

    public PointsService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addPoints(String userId, int points){
        String key = "score:" + userId;
        redisTemplate.opsForValue().increment(key, points);
        
        // PHASE 2: Mise à jour du leaderboard (ZSET)
        redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, userId, points);
    }

    public Integer getPoints(String userId){
        String key = "score:" + userId;
        Object value = redisTemplate.opsForValue().get(key);
        
        if(value != null){
            return Integer.parseInt((String) value);
        } else {
            return 0;
        }
    }

    // ========== REQUÊTES AVANCÉES PHASE 2 (Redis ZSET) ==========

    /**
     * Top N utilisateurs du leaderboard (structure ZSET optimisée)
     */
    public Set<TypedTuple<Object>> getTopUsers(int limit) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);
    }

    /**
     * Position d'un utilisateur dans le classement (0 = premier)
     */
    public Long getUserRank(String userId) {
        return redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, userId);
    }

    /**
     * Nombre total d'utilisateurs dans le leaderboard
     */
    public Long getLeaderboardSize() {
        return redisTemplate.opsForZSet().size(LEADERBOARD_KEY);
    }

    /**
     * Utilisateurs avec un score dans une fourchette
     */
    public Set<TypedTuple<Object>> getUsersByScoreRange(double minScore, double maxScore) {
        return redisTemplate.opsForZSet().rangeByScoreWithScores(LEADERBOARD_KEY, minScore, maxScore);
    }
}