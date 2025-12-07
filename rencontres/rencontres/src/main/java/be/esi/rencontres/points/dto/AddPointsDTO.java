package be.esi.rencontres.points.dto;

import lombok.Data;

@Data
public class AddPointsDTO {
    private String userId;
    private int points;

}