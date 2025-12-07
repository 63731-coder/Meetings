package be.esi.rencontres.user.repository;

import be.esi.rencontres.user.model.mongo.UserDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMongoRepository extends MongoRepository<UserDoc, String> {
}
