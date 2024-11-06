package com.example.fitness_service.model;


import com.example.fitness_service.dto.fitnessDto;
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
public class fitnessModel {
    private String fitness_Id;
    private LocalDate fitness_Date;
    private String user_Id;
    public List<fitnessDto> workoutList;

}