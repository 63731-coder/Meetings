package be.esi.rencontres.meeting.controller;

import be.esi.rencontres.meeting.dto.MeetingDTO;
import be.esi.rencontres.meeting.model.neo4j.MeetingRelationship;
import be.esi.rencontres.meeting.service.MeetingService;
import be.esi.rencontres.points.service.PointsService;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;       // Nécessaire pour lire les intérêts (MongoDB)
    private final PointsService pointsService;   // Nécessaire pour écrire les points (Redis)

    public MeetingController(MeetingService meetingService, UserService userService, PointsService pointsService) {
        this.meetingService = meetingService;
        this.userService = userService;
        this.pointsService = pointsService;
    }

    @PostMapping
    public ResponseEntity<MeetingRelationship> createMeeting(@Valid @RequestBody MeetingDTO meetingDTO) {
        try {
            // 1. Sauvegarde de la rencontre dans le Graphe (Neo4j)
            MeetingRelationship meeting = meetingService.createMeeting(
                    meetingDTO.getUserId1(),
                    meetingDTO.getUserId2(),
                    meetingDTO.getMeetingDate(),
                    meetingDTO.getLocation()
            );

            // CALCUL DES POINTS
            
            // 2. Récupération de l'utilisateur CIBLE pour analyser son profil (MongoDB)
            Optional<UserDoc> targetUserOpt = userService.getUserById(meetingDTO.getUserId2());
            
            if (targetUserOpt.isPresent()) {
                UserDoc targetUser = targetUserOpt.get();
                
                // Compter le nombre de centres d'intérêt
                int interestCount = (targetUser.getInterests() != null) ? targetUser.getInterests().size() : 0;
                
                // 3. Application de la règle de points : 10 points de base + 5 par intérêt
                int pointsToAward = 10 + (interestCount * 5);
                
                // 4. Attribution des points à l'initiateur dans le Cache (Redis)
                pointsService.addPoints(meetingDTO.getUserId1(), pointsToAward);
                
                // Petit log pour voir que ça marche dans la console
                System.out.println("✅ Rencontre créée ! " + pointsToAward + " points attribués à l'utilisateur " + meetingDTO.getUserId1());
            }
            // ------------------------------------------

            return new ResponseEntity<>(meeting, HttpStatus.CREATED);

        } catch (IllegalStateException e) {
            // Renvoie 409 si la rencontre existe déjà
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MeetingRelationship>> getAllMeetingsByUserId(@PathVariable String userId) {
        List<MeetingRelationship> meetings = meetingService.getAllMeetingsByUserId(userId);
        return new ResponseEntity<>(meetings, HttpStatus.OK);
    }

    @GetMapping("/between")
    public ResponseEntity<MeetingRelationship> getMeetingBetweenUsers(
            @RequestParam String userId1,
            @RequestParam String userId2) {
        Optional<MeetingRelationship> meeting = meetingService.getMeetingBetweenUsers(userId1, userId2);
        return meeting.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkMeetingExists(
            @RequestParam String userId1,
            @RequestParam String userId2) {
        boolean exists = meetingService.existsMeetingBetweenUsers(userId1, userId2);
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

