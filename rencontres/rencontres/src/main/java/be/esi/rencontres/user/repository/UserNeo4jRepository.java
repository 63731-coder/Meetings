package be.esi.rencontres.user.repository;

import be.esi.rencontres.user.model.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNeo4jRepository extends Neo4jRepository<UserNode, String> {

    /**
     * Recherche les utilisateurs dans une ville donnée via Neo4j
     */
    List<UserNode> findByLocalisationIgnoreCase(String localisation);
    
    /**
     * Trouve tous les utilisateurs qui ont eu des rencontres avec un utilisateur donné
     */
    @Query("MATCH (u:User {id: $userId})-[:MET]-(other:User) RETURN other")
    List<UserNode> findUsersByMeetings(@Param("userId") String userId);
}
