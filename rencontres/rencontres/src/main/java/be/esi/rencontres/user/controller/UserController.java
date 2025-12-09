package be.esi.rencontres.user.controller;

import be.esi.rencontres.user.dto.UserDTO;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Route GET / : Affiche la page Thymeleaf
     */
    @GetMapping("/")
    public String index() {
        return "users";
    }

    /**
     * Route POST /api/users : Enregistre un nouvel utilisateur (API REST)
     * Déclenche la Double Écriture vers MongoDB (profil) et Neo4j (nœud du graphe).
     *
     * @param userDTO Les données reçues, validées par @Valid
     * @return L'utilisateur enregistré et le statut 201 CREATED
     */
    @PostMapping("/api/users")
    public ResponseEntity<UserDoc> registerUser(
            @Valid @RequestBody UserDTO userDTO) {

        UserDoc userDoc = new UserDoc();
        userDoc.setUsername(userDTO.getUsername());
        userDoc.setBio(userDTO.getBio());
        userDoc.setInterests(userDTO.getInterests());

        UserDoc savedUser = userService.registerUser(userDoc);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    /**
     * Route GET /api/users/search : Recherche les utilisateurs par centre d'intérêt
     *
     * @param interest Le centre d'intérêt à rechercher
     * @return Liste des utilisateurs ayant cet intérêt
     */
    @GetMapping("/api/users/search")
    @ResponseBody
    public ResponseEntity<List<UserDoc>> searchByInterest(@RequestParam String interest) {
        List<UserDoc> users = userService.findUsersByInterest(interest.trim());
        return ResponseEntity.ok(users);
    }
}