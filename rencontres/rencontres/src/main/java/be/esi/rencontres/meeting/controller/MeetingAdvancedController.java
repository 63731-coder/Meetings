package be.esi.rencontres.meeting.controller;

import be.esi.rencontres.meeting.model.neo4j.MeetingRelationship;
import be.esi.rencontres.meeting.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller pour les requêtes avancées Neo4j (Phase 2)
 */
@RestController
@RequestMapping("/api/meetings/advanced")
public class MeetingAdvancedController {

    private final MeetingService meetingService;

    public MeetingAdvancedController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    /**
     * GET /api/meetings/advanced/count/{userId}
     * Nombre de rencontres d'un utilisateur
     */
    @GetMapping("/count/{userId}")
    public ResponseEntity<Integer> countUserMeetings(@PathVariable String userId) {
        return ResponseEntity.ok(meetingService.countMeetingsByUser(userId));
    }

    /**
     * GET /api/meetings/advanced/most-active?limit=10
     * Top utilisateurs les plus actifs
     */
    @GetMapping("/most-active")
    public ResponseEntity<List<Object>> getMostActiveUsers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(meetingService.getMostActiveUsers(limit));
    }

    /**
     * GET /api/meetings/advanced/suggestions/{userId}?limit=5
     * Recommandations basées sur rencontres communes
     */
    @GetMapping("/suggestions/{userId}")
    public ResponseEntity<List<Object>> getSuggestions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(meetingService.getSuggestionsBasedOnCommonMeetings(userId, limit));
    }

    /**
     * GET /api/meetings/advanced/recent?days=7&limit=20
     * Rencontres récentes (depuis N jours)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<MeetingRelationship>> getRecentMeetings(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "20") int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return ResponseEntity.ok(meetingService.getRecentMeetings(since, limit));
    }

    /**
     * GET /api/meetings/advanced/distance?user1=id1&user2=id2
     * Distance entre 2 utilisateurs (chemins de rencontres)
     */
    @GetMapping("/distance")
    public ResponseEntity<Integer> getDistance(
            @RequestParam String user1,
            @RequestParam String user2) {
        Integer distance = meetingService.getDistanceBetweenUsers(user1, user2);
        return ResponseEntity.ok(distance != null ? distance : -1);
    }
}
