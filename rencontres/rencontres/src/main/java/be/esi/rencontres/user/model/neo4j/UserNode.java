package be.esi.rencontres.user.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("User") // Indique que c'est un Nœud Neo4j, étiqueté "User"
public class UserNode {

    @Id
    private String id;
    
    private String username;
    
    private String bio;
    
    private List<String> interests;
}