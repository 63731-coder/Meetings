package be.esi.rencontres.points.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Repository Redis pour la gestion des points et du leaderboard
 * Abstraction des opérations Redis (Phase 2)
 */
@Repository
public class PointsRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SCORE_PREFIX = "score:";
    private static final String LEADERBOARD_KEY = "leaderboard";

    public PointsRedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========== Opérations sur les scores individuels ==========

    /**
     * Incrémente le score d'un utilisateur
     */
    public void incrementScore(String userId, int points) {
        String key = SCORE_PREFIX + userId;
        redisTemplate.opsForValue().increment(key, points);
    }

    /**
     * Récupère le score d'un utilisateur
     */
    public Integer getScore(String userId) {
        String key = SCORE_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }

    // ========== Opérations sur le leaderboard (ZSET) ==========

    /**
     * Incrémente le score d'un utilisateur dans le leaderboard
     */
    public void incrementLeaderboardScore(String userId, int points) {
        redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, userId, points);
    }

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

