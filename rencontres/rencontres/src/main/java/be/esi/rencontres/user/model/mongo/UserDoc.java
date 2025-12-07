package be.esi.rencontres.user.model.mongo;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDoc {
    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("biography")
    private String bio;

    @Field("interests")
    private List<String> interests;
}
