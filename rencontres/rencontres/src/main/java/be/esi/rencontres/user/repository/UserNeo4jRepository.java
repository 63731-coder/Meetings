package be.esi.rencontres.user.repository;

import be.esi.rencontres.user.model.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface UserNeo4jRepository extends Neo4jRepository<UserNode, String> {
}
