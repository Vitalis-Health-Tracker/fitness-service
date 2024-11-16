package com.example.fitness_service.controller;

import com.example.fitness_service.dto.ExerciseDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.service.FitnessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/health/fitness")
public class FitnessController {
    private final FitnessService fitnessService;

    public FitnessController(FitnessService fitnessService) {
        this.fitnessService = fitnessService;
    }

    @PostMapping("/{userId}/add-workout")
    public Mono<ResponseEntity<Void>> addWorkout(
            @PathVariable String userId,
            @RequestParam String workoutName,
            @RequestParam int inputReps,
            @RequestParam int inputSets,
            @RequestParam int inputDuration) {
        return fitnessService.addWorkout(userId, workoutName, inputReps, inputSets, inputDuration)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
    @PostMapping("/{userId}/add-custom-workout")
    public Mono<Void> addCustomWorkout(@PathVariable String userId, @RequestBody ExerciseDto exerciseDto) {
        return fitnessService.addCustomWorkout(userId, exerciseDto);
    }
    @PostMapping("/{userId}/save-workouts")
    public Mono<FitnessModel> saveWorkouts(@PathVariable String userId) {
        return fitnessService.saveWorkoutAndCalculateCalories(userId);
    }
    @PostMapping("/{userId}/update-workouts")
    public Mono<FitnessModel> updateWorkouts(@PathVariable String userId) {
        return fitnessService.updateWorkout(userId);
    }

    @DeleteMapping("/{userId}/delete-workout/{workoutId}")
    public Mono<ResponseEntity<String>> deleteWorkout(
            @PathVariable String userId,
            @PathVariable String workoutId) {
        return fitnessService.deleteWorkout(userId, workoutId)
                .map(updatedFitness -> ResponseEntity.ok("Workout deleted successfully"))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PutMapping("/{fitnessId}/edit-workout/{workoutId}")
    public Mono<ResponseEntity<FitnessModel>> editWorkout(
            @PathVariable String fitnessId,
            @PathVariable String workoutId,
            @RequestBody ExerciseDto updatedWorkout) {
        return fitnessService.editWorkout(fitnessId, workoutId, updatedWorkout)
                .map(ResponseEntity::ok) // Return the updated FitnessModel with HTTP 200
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(null))); // Handle errors with HTTP 400
    }

    @GetMapping("/{userId}/get-workouts")
    public Mono<ResponseEntity<List<FitnessModel>>> getWorkouts(@PathVariable String userId, @RequestParam LocalDateTime startDate, @RequestParam LocalDateTime endDate) {
        return fitnessService.getWorkoutsByWeek(userId, startDate, endDate)
                .map(fitnessModel -> ResponseEntity.ok(fitnessModel))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(null)));
    }

    @GetMapping("/{userId}/get-workout")
    public Mono<ResponseEntity<FitnessModel>> getWorkout(@PathVariable String userId) {
        return fitnessService.getWorkout(userId)
                .map(fitnessModel -> ResponseEntity.ok(fitnessModel))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(null)));
    }
}
