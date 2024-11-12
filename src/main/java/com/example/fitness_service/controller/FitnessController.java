package com.example.fitness_service.controller;

import com.example.fitness_service.dto.ExerciseDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.service.FitnessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @PostMapping("/{userId}/update-workouts")
    public Mono<ResponseEntity<FitnessModel>> addWorkoutToList(@PathVariable String userId, @RequestParam String workoutName) {
        return fitnessService.updateWorkoutList(userId, workoutName)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @DeleteMapping("/{userId}/workouts/{workoutId}")
    public Mono<ResponseEntity<String>> deleteWorkout(
            @PathVariable String userId,
            @PathVariable String workoutId) {
        return fitnessService.deleteWorkout(userId, workoutId)
                .map(updatedFitness -> ResponseEntity.ok("Workout deleted successfully"))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PutMapping("/{fitnessId}/workouts/{workoutId}")
    public Mono<ResponseEntity<String>> editWorkout(
            @PathVariable String fitnessId,
            @PathVariable String workoutId,
            @RequestBody ExerciseDto updatedWorkout) {
        return fitnessService.editWorkout(fitnessId, workoutId, updatedWorkout)
                .map(updatedFitness -> ResponseEntity.ok("Workout updated successfully"))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }


}
