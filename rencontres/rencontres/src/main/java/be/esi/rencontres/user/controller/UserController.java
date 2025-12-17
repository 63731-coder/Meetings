package be.esi.rencontres.user.controller;

import be.esi.rencontres.points.service.PointsService;
import be.esi.rencontres.user.dto.UserDTO;
import be.esi.rencontres.user.model.mongo.UserDoc;
import be.esi.rencontres.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * Combine MongoDB (infos user) et Redis (scores)
     */
    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
    
        List<UserDoc> allUsers = userService.findAll();

        List<LeaderboardEntry> leaderboard = allUsers.stream()
            .map(user -> {
            
                Integer score = pointsService.getPoints(user.getId());
                return new LeaderboardEntry(
                    user.getUsername(),
                    user.getLocalisation(),
                    score != null ? score : 0
                );
            })
            // Trier par score décroissant 
            .sorted((e1, e2) -> e2.score.compareTo(e1.score))
            
            .limit(10)
            .collect(Collectors.toList());

        // Envoyer la liste à la vue HTML
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

        if (hasInterest) {
            return ResponseEntity.ok(userService.findUsersByInterest(trimmedInterest, currentUserId));
        } else {
            return ResponseEntity.ok(userService.findUsersByLocalisation(trimmedLocalisation, currentUserId));
        }
    }

    // Petite classe interne pour transporter les données vers la vue Leaderboard ---
    public static class LeaderboardEntry {
        public String username;
        public String city;
        public Integer score;

        public LeaderboardEntry(String username, String city, Integer score) {
            this.username = username;
            this.city = city;
            this.score = score;
        }
    }
}