package be.esi.rencontres.user.repository;

import be.esi.rencontres.user.model.mongo.UserDoc;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserMongoRepository extends MongoRepository<UserDoc, String> {
    List<UserDoc> findByInterestsContainingIgnoreCase(String interest);
    List<UserDoc> findByLocalisationContainingIgnoreCase(String localisation);
    Optional<UserDoc> findByUsername(String username);

    // ========== REQUÊTES AVANCÉES PHASE 2 (Agrégations MongoDB) ==========

    /**
     * Top 10 centres d'intérêt les plus populaires
     */
    @Aggregation(pipeline = {
        "{ $unwind: '$interests' }",
        "{ $group: { _id: '$interests', count: { $sum: 1 } } }",
        "{ $sort: { count: -1 } }",
        "{ $limit: 10 }"
    })
    List<Map<String, Object>> findTopInterests();

    /**
     * Nombre d'utilisateurs par localisation
     */
    @Aggregation(pipeline = {
        "{ $group: { _id: '$localisation', userCount: { $sum: 1 } } }",
        "{ $sort: { userCount: -1 } }"
    })
    List<Map<String, Object>> countUsersByLocation();

    /**
     * Utilisateurs avec le plus d'intérêts
     */
    @Aggregation(pipeline = {
        "{ $project: { username: 1, localisation: 1, interestCount: { $size: { $ifNull: ['$interests', []] } } } }",
        "{ $sort: { interestCount: -1 } }",
        "{ $limit: 10 }"
    })
    List<Map<String, Object>> findUsersWithMostInterests();

    /**
     * Statistiques globales
     */
    @Aggregation(pipeline = {
        "{ $group: { " +
        "    _id: null, " +
        "    totalUsers: { $sum: 1 }, " +
        "    avgInterests: { $avg: { $size: { $ifNull: ['$interests', []] } } } " +
        "} }"
    })
    Map<String, Object> getGlobalStatistics();
}
