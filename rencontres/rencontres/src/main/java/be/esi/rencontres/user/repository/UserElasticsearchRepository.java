package be.esi.rencontres.user.repository;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import be.esi.rencontres.user.model.elasticsearch.UserDocument;

/**
 * Repository Elasticsearch pour la recherche plein texte sur les utilisateurs
 */
@Repository
public interface UserElasticsearchRepository extends ElasticsearchRepository<UserDocument, String> {
    
    /**
     * Recherche plein texte sur username et bio
     * @param query le texte à rechercher
     * @return liste des utilisateurs correspondants
     */
    List<UserDocument> findByUsernameContainingOrBioContaining(String usernameQuery, String bioQuery);
    
    /**
     * Recherche par centre d'intérêt exact
     * @param interest le centre d'intérêt à rechercher
     * @return liste des utilisateurs ayant ce centre d'intérêt
     */
    List<UserDocument> findByInterests(String interest);
    
    /**
     * Recherche par localisation
     * @param localisation la ville à rechercher
     * @return liste des utilisateurs dans cette ville
     */
    List<UserDocument> findByLocalisation(String localisation);
    
    /**
     * Recherche floue (tolérante aux fautes) par centre d'intérêt
     * Utilise fuzzy matching pour trouver des correspondances approximatives
     * @param interest le centre d'intérêt à rechercher (peut contenir des fautes)
     * @return liste des utilisateurs ayant un centre d'intérêt similaire
     */
    @Query("{\"bool\": {\"should\": [{\"match\": {\"interests\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}]}}")
    List<UserDocument> findByInterestsFuzzy(String interest);
    
    /**
     * Recherche floue (tolérante aux fautes) par localisation
     * Utilise fuzzy matching pour trouver des correspondances approximatives
     * @param localisation la ville à rechercher (peut contenir des fautes)
     * @return liste des utilisateurs dans une ville similaire
     */
    @Query("{\"match\": {\"localisation\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}")
    List<UserDocument> findByLocalisationFuzzy(String localisation);
}
