package com.example.fitness_service.controller;

import com.example.fitness_service.dto.ExerciseDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.service.FitnessService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/health/fitness")
public class FitnessController {


    private final FitnessService fitnessService;

    public FitnessController(FitnessService fitnessService) {
        this.fitnessService = fitnessService;
    }

    @PostMapping("/{userId}/workout")
    public Mono<FitnessModel> addWorkout(@PathVariable String userId, @RequestParam String workoutName) {
        return fitnessService.addWorkout(userId, workoutName);
    }

    @PostMapping("/workoutadd")
    public Mono<FitnessModel> addWorkout2(@RequestParam String workoutName) {
        return fitnessService.addWorkout2(workoutName);

    }

    @GetMapping("/{userId}/calories")
    public Mono<Float> calculateTotalCalories(@PathVariable String userId) {
        return fitnessService.calculateTotalCalories(userId, LocalDate.now());
    }

    @PutMapping("/{fitnessId}/workout")
    public Mono<FitnessModel> updateWorkout(@PathVariable String fitnessId, @RequestBody ExerciseDto updatedWorkout) {
        return fitnessService.updateWorkout(fitnessId, updatedWorkout);
    }

    @DeleteMapping("/{fitnessId}/workout/{workoutId}")
    public Mono<Void> deleteWorkout(@PathVariable String fitnessId, @PathVariable String workoutId) {
        return fitnessService.deleteWorkout(fitnessId, workoutId);
    }
}
