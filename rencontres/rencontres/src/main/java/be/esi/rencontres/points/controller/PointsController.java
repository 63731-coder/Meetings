package be.esi.rencontres.points.controller;

import org.springframework.web.bind.annotation.RestController;

import be.esi.rencontres.points.dto.AddPointsDTO;
import be.esi.rencontres.points.service.PointsService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/points")
public class PointsController {
    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping("/add")
    public void addPoints(@RequestBody AddPointsDTO dto) {
        pointsService.addPoints(dto.getUserId(), dto.getPoints());
    }

    @GetMapping("/{userId}")
    public Integer getScore(@PathVariable String userId) {
        return pointsService.getPoints(userId);
    }
}
