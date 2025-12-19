package be.esi.rencontres.points.controller;

import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.web.bind.annotation.RestController;

import be.esi.rencontres.points.dto.AddPointsDTO;
import be.esi.rencontres.points.service.PointsService;

import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/points")
public class PointsController {
    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping("/add")
    public void addPoints(@RequestBody AddPointsDTO dto) {
        pointsService.addPoints(dto.getUserId(), dto.getPoints());
    }

    @GetMapping("/{userId}")
    public Integer getScore(@PathVariable String userId) {
        return pointsService.getPoints(userId);
    }

    // ========== ENDPOINTS PHASE 2 (Redis ZSET) ==========

    @GetMapping("/leaderboard/top")
    public Set<TypedTuple<Object>> getTopUsers(@RequestParam(defaultValue = "10") int limit) {
        return pointsService.getTopUsers(limit);
    }

    @GetMapping("/rank/{userId}")
    public Long getUserRank(@PathVariable String userId) {
        return pointsService.getUserRank(userId);
    }

    @GetMapping("/leaderboard/size")
    public Long getLeaderboardSize() {
        return pointsService.getLeaderboardSize();
    }

    @GetMapping("/range")
    public Set<TypedTuple<Object>> getUsersByScoreRange(
            @RequestParam double min,
            @RequestParam double max) {
        return pointsService.getUsersByScoreRange(min, max);
    }
}
