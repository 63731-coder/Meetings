package be.esi.rencontres.user.model.elasticsearch;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modèle Elasticsearch pour les utilisateurs
 * Optimisé pour la recherche plein texte sur username, bio et interests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users")
public class UserDocument {
    
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String username;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String bio;

    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> interests;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String localisation;
}
