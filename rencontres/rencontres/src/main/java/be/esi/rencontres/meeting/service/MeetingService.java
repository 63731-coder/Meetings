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
}
