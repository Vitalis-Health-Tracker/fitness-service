package com.example.fitness_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FitnessDto {
    private String workoutName;
    private Integer reps;
    private Integer duration;
    private Integer sets;
    private String workoutType;
    private Float caloriesBurned;

}
