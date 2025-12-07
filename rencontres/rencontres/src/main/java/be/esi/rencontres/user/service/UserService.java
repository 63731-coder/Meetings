package be.esi.rencontres.user.service;

import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.repository.UserMongoRepository;
import org.springframework.stereotype.Service;
import org.neo4j.driver.Driver;

@Service
public class UserService {

    private final UserMongoRepository userRepository;
    private final Driver neo4jDriver;

    public UserService(UserMongoRepository userRepository, Driver neo4jDriver) {
        this.userRepository = userRepository;
        this.neo4jDriver = neo4jDriver;
    }

    /**
     * Implémenter la logique de "Double Écriture" ici
     *
     * @param user le document utilisateur à enregistrer (-> MongoDB)
     * @return UserDoc - l'utilisateur enregistré (-> MongoDB)
     */
    public UserDoc registerUser(UserDoc user) {
        // Sauvegarde dans MongoDB le profil utilisateur
        UserDoc savedUser = userRepository.save(user);

        // Création du nœud Neo4j (Graphe social)
        createNeo4jNode(savedUser.getId());

        return savedUser;
    }

    /**
     * Crée un nœud User dans Neo4j en utilisant uniquement l'ID.
     *
     * @param userId l'ID de l'utilisateur
     */
    private void createNeo4jNode(String userId) {
        String cypherQuery = "CREATE (u:User {id: $userId})";

        try (org.neo4j.driver.Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypherQuery,
                        // Map pour passer le paramètre userId de manière sécurisée
                        java.util.Map.of("userId", userId));
                return null; // Les transactions d'écriture ne retournent souvent pas de résultat ici
            });
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du noeud Neo4j pour l'utilisateur " + userId
                    + ". La synchronisation a échoué !: " + e.getMessage());
        }
    }
}