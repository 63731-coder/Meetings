package be.esi.rencontres.user.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import be.esi.rencontres.points.service.PointsService;
import be.esi.rencontres.user.dto.LeaderboardDTO;
import be.esi.rencontres.user.dto.UserDTO;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class UserController {

    private final UserService userService;
    private final PointsService pointsService;

    public UserController(UserService userService, PointsService pointsService) {
        this.userService = userService;
        this.pointsService = pointsService;
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
     * Route GET /search : Affiche la page de recherche (protégée)
     */
    @GetMapping("/search")
    public String search() {
        return "search";
    }

    /**
     * Route GET /leaderboard : Affiche le classement Top 10
     * PHASE 2: Utilise Redis ZSET (requête avancée)
     */
    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        // Récupère le Top 10 depuis Redis ZSET
        var topUsers = pointsService.getTopUsers(10);
        
        List<LeaderboardDTO> leaderboard = topUsers.stream()
            .map(entry -> {
                String userId = (String) entry.getValue();
                Integer score = entry.getScore().intValue();
                
                // Récupère les infos depuis MongoDB
                Optional<UserDoc> userOpt = userService.getUserById(userId);
                if (userOpt.isPresent()) {
                    UserDoc user = userOpt.get();
                    return new LeaderboardDTO(
                        user.getUsername(),
                        user.getLocalisation(),
                        score
                    );
                }
                return null;
            })
            .filter(entry -> entry != null)
            .collect(Collectors.toList());

        model.addAttribute("leaderboard", leaderboard);
        return "leaderboard";
    }

    /**
     * Route GET /users/detail/{id} : Affiche le détail d'un utilisateur
     */
    @GetMapping("/users/detail/{id}")
    public String userDetail(@PathVariable String id, Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/";
        }

        Optional<UserDoc> userOpt = userService.getUserById(id);

        if (userOpt.isPresent()) {
            model.addAttribute("targetUser", userOpt.get());
            return "detail";
        } else {
            return "redirect:/search";
        }
    }

    /**
     * Route POST /api/users : Enregistrement
     */
    @PostMapping("/api/users")
    public ResponseEntity<UserDoc> registerUser(@Valid @RequestBody UserDTO userDTO) {
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
     * Route GET /api/users/search : API de recherche
     * Utilise Elasticsearch avec fuzzy matching pour tolérer les fautes d'orthographe
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

        if (!hasInterest && !hasLoc) return ResponseEntity.badRequest().build();
        if (hasInterest && hasLoc) return ResponseEntity.badRequest().build();

        String currentUserId = (String) session.getAttribute("userId");

        // Utiliser Elasticsearch avec fuzzy matching pour tolérer les fautes d'orthographe
        if (hasInterest) {
            return ResponseEntity.ok(userService.findUsersByInterestElasticsearch(trimmedInterest, currentUserId));
        } else {
            return ResponseEntity.ok(userService.findUsersByLocalisationElasticsearch(trimmedLocalisation, currentUserId));
        }
    }

    /**
     * Route GET /api/users/fulltext-search : API de recherche plein texte avec Elasticsearch
     * Recherche avancée dans username, bio et centres d'intérêt
     */
    @GetMapping("/api/users/fulltext-search")
    @ResponseBody
    public ResponseEntity<List<UserDoc>> fullTextSearch(
            @RequestParam String query,
            HttpSession session) {

        if (query == null || query.trim().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String currentUserId = (String) session.getAttribute("userId");
        List<UserDoc> results = userService.fullTextSearch(query.trim(), currentUserId);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Route GET /statistics : Affiche la page des statistiques globales
     */
    @GetMapping("/statistics")
    public String statistics(Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/";
        }
        return "statistics";
    }

    /**
     * Route GET /api/users/{userId}/meetings : Utilisateurs rencontrés
     * Utilise la requête Neo4j findUsersByMeetings
     */
    @GetMapping("/api/users/{userId}/meetings")
    @ResponseBody
    public ResponseEntity<List<UserDoc>> getUserMeetings(@PathVariable String userId) {
        List<UserDoc> metUsers = userService.findUsersMetWith(userId);
        return ResponseEntity.ok(metUsers);
    }
}