package be.esi.rencontres.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    // l'id est généré par la base de données, pas besoin de le valider ici

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @Size(max = 255, message = "Bio must be less than 255 characters")
    private String bio;

    private List<String> interests;

    private String localisation;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}