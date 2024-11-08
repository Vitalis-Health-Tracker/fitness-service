package com.example.fitness_service.controller;

import com.example.fitness_service.dto.FitnessDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.service.FitnessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/health/fitness")
public class FitnessController {

    @Autowired
    private FitnessService fitnessService;

    @GetMapping("/workout-details/{workoutName}")
    public ResponseEntity<FitnessDto> fetchWorkoutDetails(@PathVariable String workoutName) {
        FitnessDto workoutDetails = fitnessService.getWorkoutDetails(workoutName).block();
        return workoutDetails != null
                ? ResponseEntity.ok(workoutDetails)
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/workouts/{userId}")
    public ResponseEntity<Mono<FitnessModel>> addWorkoutsForUser(@PathVariable String userId, @RequestBody List<FitnessDto> workouts) {
        Mono<FitnessModel> fitnessModel = fitnessService.addFitnessEntry(userId, workouts);
        return ResponseEntity.ok(fitnessModel);
    }

    @GetMapping("/workouts/user/{userId}")
    public ResponseEntity<List<FitnessModel>> getFitnessDetailsByUserId(@PathVariable String userId) {
        List<FitnessModel> fitnessDetails = fitnessService.getFitnessDetailsByUserId(userId);
        return fitnessDetails.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(fitnessDetails);
    }
}
