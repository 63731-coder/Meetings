package be.esi.rencontres.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour transporter les données du leaderboard vers la vue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDTO {
    private String username;
    private String city;
    private Integer score;
}
