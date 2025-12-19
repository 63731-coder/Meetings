package be.esi.rencontres.meeting.service;

import be.esi.rencontres.meeting.model.neo4j.MeetingRelationship;
import be.esi.rencontres.meeting.repository.MeetingNeo4jRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    private final MeetingNeo4jRepository meetingNeo4jRepository;

    public MeetingService(MeetingNeo4jRepository meetingNeo4jRepository) {
        this.meetingNeo4jRepository = meetingNeo4jRepository;
    }

    public MeetingRelationship createMeeting(String userId1, String userId2, LocalDateTime meetingDate, String location) {
        // Validation: la date ne peut pas être dans le passé
        if (meetingDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de rencontre ne peut pas être dans le passé");
        }
        
        if (existsMeetingBetweenUsers(userId1, userId2)) {
            throw new IllegalStateException("A meeting already exists between these users");
        }
        return meetingNeo4jRepository.createMeeting(userId1, userId2, meetingDate, location);
    }

    public List<MeetingRelationship> getAllMeetingsByUserId(String userId) {
        return meetingNeo4jRepository.findAllMeetingsByUserId(userId);
    }

    public Optional<MeetingRelationship> getMeetingBetweenUsers(String userId1, String userId2) {
        return meetingNeo4jRepository.findMeetingBetweenUsers(userId1, userId2);
    }

    public boolean existsMeetingBetweenUsers(String userId1, String userId2) {
        return meetingNeo4jRepository.existsMeetingBetweenUsers(userId1, userId2);
    }

    public void deleteMeeting(Long meetingId) {
        meetingNeo4jRepository.deleteById(meetingId);
    }

    // ========== MÉTHODES PHASE 2 (Requêtes avancées Neo4j) ==========

    public int countMeetingsByUser(String userId) {
        return meetingNeo4jRepository.countMeetingsByUser(userId);
    }

    public List<Object> getMostActiveUsers(int limit) {
        return meetingNeo4jRepository.findMostActiveUsers(limit);
    }

    public List<Object> getSuggestionsBasedOnCommonMeetings(String userId, int limit) {
        return meetingNeo4jRepository.findSuggestionsBasedOnCommonMeetings(userId, limit);
    }

    public List<MeetingRelationship> getRecentMeetings(LocalDateTime since, int limit) {
        return meetingNeo4jRepository.findRecentMeetings(since, limit);
    }

    public Integer getDistanceBetweenUsers(String userId1, String userId2) {
        return meetingNeo4jRepository.findDistanceBetweenUsers(userId1, userId2);
    }
}
