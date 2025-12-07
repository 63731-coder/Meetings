package be.esi.rencontres.meeting.model.neo4j;

import be.esi.rencontres.user.model.neo4j.UserNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RelationshipProperties
public class MeetingRelationship {

    @Id
    @GeneratedValue
    private Long id;

    private LocalDateTime meetingDate;

    private String location;

    @TargetNode
    private UserNode targetUser;

    public MeetingRelationship(LocalDateTime meetingDate, String location, UserNode targetUser) {
        this.meetingDate = meetingDate;
        this.location = location;
        this.targetUser = targetUser;
    }
}
