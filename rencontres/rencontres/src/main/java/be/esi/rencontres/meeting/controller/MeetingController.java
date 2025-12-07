package be.esi.rencontres.meeting.controller;

import be.esi.rencontres.meeting.dto.MeetingDTO;
import be.esi.rencontres.meeting.model.neo4j.MeetingRelationship;
import be.esi.rencontres.meeting.service.MeetingService;
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

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public ResponseEntity<MeetingRelationship> createMeeting(@Valid @RequestBody MeetingDTO meetingDTO) {
        try {
            MeetingRelationship meeting = meetingService.createMeeting(
                    meetingDTO.getUserId1(),
                    meetingDTO.getUserId2(),
                    meetingDTO.getMeetingDate(),
                    meetingDTO.getLocation()
            );
            return new ResponseEntity<>(meeting, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
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

