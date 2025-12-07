package be.esi.rencontres.user.service;

import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.model.neo4j.UserNode;
import be.esi.rencontres.user.repository.UserMongoRepository;
import be.esi.rencontres.user.repository.UserNeo4jRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMongoRepository userMongoRepository;
    private final UserNeo4jRepository userNeo4jRepository;

    public UserService(UserMongoRepository userMongoRepository, UserNeo4jRepository userNeo4jRepository) {
        this.userMongoRepository = userMongoRepository;
        this.userNeo4jRepository = userNeo4jRepository;
    }

    /**
     * Implémenter la logique de "Double Écriture" ici
     *
     * @param user le document utilisateur à enregistrer (-> MongoDB)
     * @return UserDoc - l'utilisateur enregistré (-> MongoDB)
     */
    public UserDoc registerUser(UserDoc user) {
        UserDoc savedUser = userMongoRepository.save(user);
        createNeo4jNode(savedUser.getId());

        return savedUser;
    }

    /**
     * Crée un nœud User dans Neo4j en utilisant uniquement l'ID.
     *
     * @param userId l'ID de l'utilisateur
     */
    private void createNeo4jNode(String userId) {
        UserNode userNode = new UserNode(userId);
        userNeo4jRepository.save(userNode);
    }
}