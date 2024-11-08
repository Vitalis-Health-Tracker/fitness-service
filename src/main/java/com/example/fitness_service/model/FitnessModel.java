package com.example.fitness_service.model;


import com.example.fitness_service.dto.FitnessDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Fitness_Service")
public class FitnessModel {
    private String fitnessId;
    private LocalDate fitnessDate;
    private String userId;
    public List<FitnessDto> workoutList;
    public Float totalCaloriesBurned;
}