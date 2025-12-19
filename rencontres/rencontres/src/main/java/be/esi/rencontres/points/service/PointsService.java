package be.esi.rencontres.points.service;

import be.esi.rencontres.points.repository.PointsRedisRepository;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service métier pour la gestion des points
 * Utilise PointsRedisRepository pour abstraire les opérations Redis
 */
@Service
public class PointsService {

    private final PointsRedisRepository pointsRedisRepository;

    public PointsService(PointsRedisRepository pointsRedisRepository) {
        this.pointsRedisRepository = pointsRedisRepository;
    }

    public void addPoints(String userId, int points) {
        // Mise à jour du score individuel
        pointsRedisRepository.incrementScore(userId, points);
        
        // PHASE 2: Mise à jour du leaderboard (ZSET)
        pointsRedisRepository.incrementLeaderboardScore(userId, points);
    }

    public Integer getPoints(String userId) {
        return pointsRedisRepository.getScore(userId);
    }

    // ========== REQUÊTES AVANCÉES PHASE 2 (Redis ZSET) ==========

    /**
     * Top N utilisateurs du leaderboard (structure ZSET optimisée)
     */
    public Set<TypedTuple<Object>> getTopUsers(int limit) {
        return pointsRedisRepository.getTopUsers(limit);
    }

    /**
     * Position d'un utilisateur dans le classement (0 = premier)
     */
    public Long getUserRank(String userId) {
        return pointsRedisRepository.getUserRank(userId);
    }

    /**
     * Nombre total d'utilisateurs dans le leaderboard
     */
    public Long getLeaderboardSize() {
        return pointsRedisRepository.getLeaderboardSize();
    }

    /**
     * Utilisateurs avec un score dans une fourchette
     */
    public Set<TypedTuple<Object>> getUsersByScoreRange(double minScore, double maxScore) {
        return pointsRedisRepository.getUsersByScoreRange(minScore, maxScore);
    }
}