package be.esi.rencontres.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import be.esi.rencontres.user.model.elasticsearch.UserDocument;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.model.neo4j.UserNode;
import be.esi.rencontres.user.repository.UserElasticsearchRepository;
import be.esi.rencontres.user.repository.UserMongoRepository;
import be.esi.rencontres.user.repository.UserNeo4jRepository;

@Service
public class UserService {

    private final UserMongoRepository userMongoRepository;
    private final UserNeo4jRepository userNeo4jRepository;
    private final UserElasticsearchRepository userElasticsearchRepository;

    public UserService(UserMongoRepository userMongoRepository, 
                       UserNeo4jRepository userNeo4jRepository,
                       UserElasticsearchRepository userElasticsearchRepository) {
        this.userMongoRepository = userMongoRepository;
        this.userNeo4jRepository = userNeo4jRepository;
        this.userElasticsearchRepository = userElasticsearchRepository;
    }

    /**
     * Implémenter la logique de "Triple Écriture" ici
     * MongoDB (données complètes) + Neo4j (relations) + Elasticsearch (recherche)
     *
     * @param user le document utilisateur à enregistrer (-> MongoDB)
     * @return UserDoc - l'utilisateur enregistré (-> MongoDB)
     */
    public UserDoc registerUser(UserDoc user) {
        // 1. Sauvegarder dans MongoDB
        UserDoc savedUser = userMongoRepository.save(user);
        
        // 2. Créer le nœud Neo4j
        createNeo4jNode(savedUser);
        
        // 3. Indexer dans Elasticsearch
        indexUserInElasticsearch(savedUser);
        
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
     * Indexe un utilisateur dans Elasticsearch pour la recherche plein texte.
     *
     * @param user l'utilisateur à indexer
     */
    private void indexUserInElasticsearch(UserDoc user) {
        UserDocument userDocument = new UserDocument(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getInterests(),
                user.getLocalisation()
        );
        userElasticsearchRepository.save(userDocument);
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


    /**
     * Récupère un utilisateur par son ID (MongoDB)
     * Utile pour afficher la page de détail
     */
    public java.util.Optional<UserDoc> getUserById(String id) {
        return userMongoRepository.findById(id);
    }

    public List<UserDoc> findAll() {
        return userMongoRepository.findAll();
    }

    /**
     * Recherche plein texte avancée avec Elasticsearch
     * Recherche dans username, bio et centres d'intérêt
     *
     * @param query le texte à rechercher
     * @param excludeUserId ID de l'utilisateur à exclure des résultats (peut être null)
     * @return liste des utilisateurs correspondants avec leurs profils complets
     */
    public List<UserDoc> fullTextSearch(String query, String excludeUserId) {
        // Rechercher dans Elasticsearch (plus rapide pour recherche plein texte)
        List<UserDocument> elasticsearchResults = 
            userElasticsearchRepository.findByUsernameContainingOrBioContaining(query, query);
        
        // Récupérer les profils complets depuis MongoDB
        List<String> userIds = elasticsearchResults.stream()
                .map(UserDocument::getId)
                .filter(id -> excludeUserId == null || !id.equals(excludeUserId))
                .collect(Collectors.toList());
        
        return userMongoRepository.findAllById(userIds);
    }

    /**
     * Recherche avancée par centre d'intérêt avec Elasticsearch
     * Plus performant que MongoDB pour les recherches complexes
     * Utilise fuzzy matching pour tolérer les fautes d'orthographe
     *
     * @param interest le centre d'intérêt à rechercher
     * @param excludeUserId ID de l'utilisateur à exclure des résultats (peut être null)
     * @return liste des utilisateurs correspondants
     */
    public List<UserDoc> findUsersByInterestElasticsearch(String interest, String excludeUserId) {
        // Utiliser la recherche floue pour tolérer les fautes d'orthographe
        List<UserDocument> elasticsearchResults = 
            userElasticsearchRepository.findByInterestsFuzzy(interest);
        
        List<String> userIds = elasticsearchResults.stream()
                .map(UserDocument::getId)
                .filter(id -> excludeUserId == null || !id.equals(excludeUserId))
                .collect(Collectors.toList());
        
        return userMongoRepository.findAllById(userIds);
    }
    
    /**
     * Recherche avancée par localisation avec Elasticsearch
     * Utilise fuzzy matching pour tolérer les fautes d'orthographe
     *
     * @param localisation la ville à rechercher
     * @param excludeUserId ID de l'utilisateur à exclure des résultats (peut être null)
     * @return liste des utilisateurs correspondants
     */
    public List<UserDoc> findUsersByLocalisationElasticsearch(String localisation, String excludeUserId) {
        // Utiliser la recherche floue pour tolérer les fautes d'orthographe
        List<UserDocument> elasticsearchResults = 
            userElasticsearchRepository.findByLocalisationFuzzy(localisation);
        
        List<String> userIds = elasticsearchResults.stream()
                .map(UserDocument::getId)
                .filter(id -> excludeUserId == null || !id.equals(excludeUserId))
                .collect(Collectors.toList());
        
        return userMongoRepository.findAllById(userIds);
    }
}