package be.esi.rencontres.user.controller;

import be.esi.rencontres.user.dto.UserDTO;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Route POST /users : Enregistre un nouvel utilisateur.
     * Déclenche la Double Écriture vers MongoDB (profil) et Neo4j (nœud du graphe).
     *
     * @param userDTO Les données reçues, validées par @Valid
     * @return L'utilisateur enregistré et le statut 201 CREATED
     */
    @PostMapping("/users")
    public ResponseEntity<UserDoc> registerUser(
            @Valid @RequestBody UserDTO userDTO) { // @Valid lance la vérification des @NotBlank et @Size

        UserDoc userDoc = new UserDoc(); //id auto-généré
        userDoc.setUsername(userDTO.getUsername());
        userDoc.setBio(userDTO.getBio());
        userDoc.setInterests(userDTO.getInterests());

        // 2. Appel du service pour exécuter la logique (Mongo Save + Neo4j Create)
        UserDoc savedUser = userService.registerUser(userDoc);

        // 3. Retourne le document créé avec le statut HTTP 201 (CREATED)
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }
}