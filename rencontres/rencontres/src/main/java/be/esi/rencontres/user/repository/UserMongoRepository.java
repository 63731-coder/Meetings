package be.esi.rencontres.user.repository;

import be.esi.rencontres.user.model.mongo.UserDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMongoRepository extends MongoRepository<UserDoc, String> {
    List<UserDoc> findByInterestsContainingIgnoreCase(String interest);
}
