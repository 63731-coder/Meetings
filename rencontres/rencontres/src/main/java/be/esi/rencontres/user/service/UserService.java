package be.esi.rencontres.user.service;

import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.model.neo4j.UserNode;
import be.esi.rencontres.user.repository.UserMongoRepository;
import be.esi.rencontres.user.repository.UserNeo4jRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
        createNeo4jNode(savedUser);

        return savedUser;
    }

    /**
     * Crée un nœud User dans Neo4j avec toutes les informations.
     *
     * @param user l'utilisateur avec toutes ses données
     */
    private void createNeo4jNode(UserDoc user) {
        UserNode userNode = new UserNode(user.getId(), user.getUsername(), user.getBio(), user.getInterests());
        userNeo4jRepository.save(userNode);
    }

    /**
     * Recherche les utilisateurs ayant un centre d'intérêt spécifique
     *
     * @param interest le centre d'intérêt à rechercher
     * @return liste des utilisateurs correspondants
     */
    public List<UserDoc> findUsersByInterest(String interest) {
        return userMongoRepository.findByInterestsContainingIgnoreCase(interest);
    }
}