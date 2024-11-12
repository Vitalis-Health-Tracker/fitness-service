package com.example.fitness_service.controller;

import com.example.fitness_service.dto.ExerciseDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.service.FitnessService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/health/fitness")
public class FitnessController {


    private final FitnessService fitnessService;

    public FitnessController(FitnessService fitnessService) {
        this.fitnessService = fitnessService;
    }

    @PostMapping("/{userId}/workout")
    public Mono<Void> addWorkout(@PathVariable String userId, @RequestParam String workoutName) {
        return fitnessService.addWorkout(userId, workoutName);
    }
    @PostMapping("/{userId}/save-workouts")
    public Mono<FitnessModel> saveWorkouts(@PathVariable String userId) {
        return fitnessService.saveWorkoutAndCalculateCalories(userId);
    }





    @PutMapping("/{userId}/update-workout")
    public Mono<FitnessModel> updateWorkout(@PathVariable String userId) {
        return fitnessService.updateWorkoutAndCalculateCalories(userId);
    }

    @DeleteMapping("/{fitnessId}/workout/{workoutId}")
    public Mono<Void> deleteWorkout(@PathVariable String fitnessId, @PathVariable String workoutId) {
        return fitnessService.deleteWorkout(fitnessId, workoutId);
    }
}
