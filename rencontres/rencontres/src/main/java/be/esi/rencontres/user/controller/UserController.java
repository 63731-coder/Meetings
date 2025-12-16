package be.esi.rencontres.user.controller;

import be.esi.rencontres.user.dto.UserDTO;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import java.util.List;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Route GET / : Affiche la page de login
     */
    @GetMapping("/")
    public String index() {
        return "login";
    }

    /**
     * Route GET /register : Affiche la page d'inscription
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * Route GET /search : Affiche la page de recherche (protectée)
     */
    @GetMapping("/search")
    public String search() {
        return "search";
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
        userDoc.setLocalisation(userDTO.getLocalisation());
        userDoc.setPassword(userDTO.getPassword());

        UserDoc savedUser = userService.registerUser(userDoc);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    /**
     * Route GET /api/users/search : Recherche des utilisateurs par centre d'intérêt ou par ville
     * Fournir exactement un des deux paramètres: interest OU localisation.
     *
     * @param interest Centre d'intérêt à rechercher (optionnel)
     * @param localisation     Ville (localisation) à rechercher (optionnel)
     * @return Liste des utilisateurs correspondants
     */
    @GetMapping("/api/users/search")
    @ResponseBody
    public ResponseEntity<List<UserDoc>> search(
            @RequestParam(required = false) String interest,
            @RequestParam(required = false) String localisation,
            HttpSession session) {

        String trimmedInterest = interest != null ? interest.trim() : null;
        String trimmedLocalisation = localisation != null ? localisation.trim() : null;
        if ((trimmedLocalisation == null || trimmedLocalisation.isBlank()) && localisation != null) {
            trimmedLocalisation = localisation.trim();
        }

        boolean hasInterest = trimmedInterest != null && !trimmedInterest.isBlank();
        boolean hasLoc = trimmedLocalisation != null && !trimmedLocalisation.isBlank();

        if (!hasInterest && !hasLoc) {
            return ResponseEntity.badRequest().build();
        }
        if (hasInterest && hasLoc) {
            return ResponseEntity.badRequest().build();
        }

        // Récupérer l'ID de l'utilisateur connecté pour l'exclure des résultats
        String currentUserId = (String) session.getAttribute("userId");

        if (hasInterest) {
            return ResponseEntity.ok(userService.findUsersByInterest(trimmedInterest, currentUserId));
        } else {
            return ResponseEntity.ok(userService.findUsersByLocalisation(trimmedLocalisation, currentUserId));
        }
    }
}