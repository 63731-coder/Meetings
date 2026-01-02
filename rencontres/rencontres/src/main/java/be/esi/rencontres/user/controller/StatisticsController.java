package be.esi.rencontres.user.controller;

import be.esi.rencontres.user.repository.UserMongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller pour les statistiques MongoDB (Phase 2)
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    
    private final UserMongoRepository userMongoRepository;

    public StatisticsController(UserMongoRepository userMongoRepository) {
        this.userMongoRepository = userMongoRepository;
    }

    /**
     * GET /api/statistics/top-interests
     * Top 10 centres d'intérêt populaires (Agrégation MongoDB)
     */
    @GetMapping("/top-interests")
    public ResponseEntity<List<Map<String, Object>>> getTopInterests() {
        return ResponseEntity.ok(userMongoRepository.findTopInterests());
    }

    /**
     * GET /api/statistics/users-by-location
     * Répartition des utilisateurs par localisation (Agrégation MongoDB)
     */
    @GetMapping("/users-by-location")
    public ResponseEntity<List<Map<String, Object>>> getUsersByLocation() {
        return ResponseEntity.ok(userMongoRepository.countUsersByLocation());
    }

    /**
     * GET /api/statistics/most-interests
     * Utilisateurs avec le plus de centres d'intérêt (Agrégation MongoDB)
     */
    @GetMapping("/most-interests")
    public ResponseEntity<List<Map<String, Object>>> getUsersWithMostInterests() {
        return ResponseEntity.ok(userMongoRepository.findUsersWithMostInterests());
    }

    /**
     * GET /api/statistics/global
     * Statistiques globales (Agrégation MongoDB)
     */
    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        return ResponseEntity.ok(userMongoRepository.getGlobalStatistics());
    }
}
