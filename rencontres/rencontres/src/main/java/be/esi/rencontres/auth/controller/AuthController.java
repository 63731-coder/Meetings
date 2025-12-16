package be.esi.rencontres.auth.controller;

import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.repository.UserMongoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMongoRepository userMongoRepository;

    public AuthController(UserMongoRepository userMongoRepository) {
        this.userMongoRepository = userMongoRepository;
    }

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> credentials,
            HttpSession session) {

        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<UserDoc> userOpt = userMongoRepository.findByUsername(username);

        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Nom d'utilisateur ou mot de passe incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Store user in session
        session.setAttribute("userId", userOpt.get().getId());
        session.setAttribute("username", userOpt.get().getUsername());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Connexion réussie");
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    /**
     * Check if user is logged in
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, String>> check(HttpSession session) {
        String userId = (String) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, String> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", (String) session.getAttribute("username"));
        return ResponseEntity.ok(response);
    }
}
