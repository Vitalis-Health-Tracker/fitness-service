package com.example.fitness_service.service;

import com.example.fitness_service.dto.FitnessDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.repository.FitnessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FitnessService {
    @Autowired
    FitnessRepository fitnessRepository;

    RestClient restClient = RestClient.create();

    public FitnessDto getWorkoutDetails(String workoutName){
        return restClient.get()
                .uri("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"+workoutName)
                .retrieve()
                .body(FitnessDto.class);
    }

    private float calculateCalories(FitnessDto workout) {
        return workout.getReps() * workout.getSets() * workout.getDuration() * 0.1f;
    }

    public FitnessModel addFitnessEntry(String userId, List<FitnessDto> workouts) {
        float totalCalories = 0;
        for (FitnessDto workout : workouts) {
            float calories = calculateCalories(workout);
            workout.setCaloriesBurned(calories);
            totalCalories += calories;
        }

        FitnessModel fitnessModel = new FitnessModel();
        fitnessModel.setUserId(userId);
        fitnessModel.setFitnessDate(LocalDate.now());
        fitnessModel.setWorkoutList(workouts);

        return fitnessRepository.save(fitnessModel);
    }

    public Optional<FitnessModel> getFitnessDetailsByUserId(String userId) {
        return fitnessRepository.findById(userId);
    }



}
