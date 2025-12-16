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
        // Les relations entre utilisateurs seront créées via les meetings

        return savedUser;
    }

    /**
     * Crée un nœud User dans Neo4j avec toutes les informations.
     *
     * @param user l'utilisateur avec toutes ses données
     */
    private void createNeo4jNode(UserDoc user) {
        UserNode userNode = new UserNode(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getInterests(),
                user.getLocalisation()
        );
        userNeo4jRepository.save(userNode);
    }

    /**
     * Recherche les utilisateurs ayant un centre d'intérêt spécifique
     *
     * @param interest le centre d'intérêt à rechercher
     * @param excludeUserId ID de l'utilisateur à exclure des résultats (peut être null)
     * @return liste des utilisateurs correspondants
     */
    public List<UserDoc> findUsersByInterest(String interest, String excludeUserId) {
        List<UserDoc> users = userMongoRepository.findByInterestsContainingIgnoreCase(interest);
        if (excludeUserId != null) {
            users = users.stream()
                    .filter(user -> !user.getId().equals(excludeUserId))
                    .toList();
        }
        return users;
    }

    /**
     * Recherche les utilisateurs par ville (localisation) en utilisant Neo4j
     * Retourne les documents MongoDB correspondants
     *
     * @param localisation la ville à rechercher
     * @param excludeUserId ID de l'utilisateur à exclure des résultats (peut être null)
     * @return liste des utilisateurs correspondants
     */
    public List<UserDoc> findUsersByLocalisation(String localisation, String excludeUserId) {
        // Utiliser Neo4j pour trouver les utilisateurs par ville
        List<UserNode> nodesInCity = userNeo4jRepository.findByLocalisationIgnoreCase(localisation);
        
        // Récupérer les profils complets depuis MongoDB
        List<String> userIds = nodesInCity.stream()
                .map(UserNode::getId)
                .filter(id -> excludeUserId == null || !id.equals(excludeUserId))
                .toList();
        return userMongoRepository.findAllById(userIds);
    }
}