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

    // ========== REQUÊTES AVANCÉES PHASE 2 ==========

    /**
     * Compte le nombre de rencontres d'un utilisateur (degré du nœud)
     */
    @Query("MATCH (u:User {id: $userId})-[:MET]-() " +
           "RETURN count(*) as meetingCount")
    int countMeetingsByUser(@Param("userId") String userId);

    /**
     * Top utilisateurs les plus actifs (le plus de rencontres)
     */
    @Query("MATCH (u:User)-[m:MET]-() " +
           "RETURN u.id as userId, count(m) as meetingCount " +
           "ORDER BY meetingCount DESC " +
           "LIMIT $limit")
    List<Object> findMostActiveUsers(@Param("limit") int limit);

    /**
     * Recommandations: personnes qui ont rencontré les mêmes personnes que moi
     * (connexions à distance 2 via rencontres communes)
     */
    @Query("MATCH (me:User {id: $userId})-[:MET]-(common)-[:MET]-(suggestion) " +
           "WHERE me <> suggestion " +
           "AND NOT (me)-[:MET]-(suggestion) " +
           "RETURN DISTINCT suggestion.id as userId, count(common) as commonMeetings " +
           "ORDER BY commonMeetings DESC " +
           "LIMIT $limit")
    List<Object> findSuggestionsBasedOnCommonMeetings(
            @Param("userId") String userId,
            @Param("limit") int limit);

    /**
     * Rencontres récentes (depuis une date donnée)
     */
    @Query("MATCH (u1:User)-[m:MET]-(u2:User) " +
           "WHERE m.meetingDate > $since " +
           "RETURN m, u1, u2 " +
           "ORDER BY m.meetingDate DESC " +
           "LIMIT $limit")
    List<MeetingRelationship> findRecentMeetings(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);

    /**
     * Trouve le chemin de rencontres le plus court entre 2 personnes
     */
    @Query("MATCH path = shortestPath((u1:User {id: $userId1})-[:MET*]-(u2:User {id: $userId2})) " +
           "RETURN length(path) as distance")
    Integer findDistanceBetweenUsers(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2);
}
