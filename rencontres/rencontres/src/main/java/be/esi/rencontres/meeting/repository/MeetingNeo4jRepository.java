package be.esi.rencontres.meeting.repository;

import be.esi.rencontres.meeting.model.neo4j.MeetingRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingNeo4jRepository extends Neo4jRepository<MeetingRelationship, Long> {

    @Query("MATCH (u1:User {id: $userId1}), (u2:User {id: $userId2}) " +
           "CREATE (u1)-[m:MET {meetingDate: $meetingDate, location: $location}]->(u2) " +
           "RETURN m")
    MeetingRelationship createMeeting(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2,
            @Param("meetingDate") LocalDateTime meetingDate,
            @Param("location") String location);

    @Query("MATCH (u:User {id: $userId})-[m:MET]->(other:User) " +
           "RETURN m, other")
    List<MeetingRelationship> findAllMeetingsByUserId(@Param("userId") String userId);

    @Query("MATCH (u1:User {id: $userId1})-[m:MET]-(u2:User {id: $userId2}) " +
           "RETURN m")
    Optional<MeetingRelationship> findMeetingBetweenUsers(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2);

    @Query("MATCH (u1:User {id: $userId1})-[m:MET]-(u2:User {id: $userId2}) " +
           "RETURN count(m) > 0")
    boolean existsMeetingBetweenUsers(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2);
}
