package com.example.fitness_service.controller;

import com.example.fitness_service.dto.FitnessDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.service.FitnessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/health/fitness")
public class FitnessController {
    @Autowired
    FitnessService fitnessService;

    @PostMapping("/workouts/{userId}")
    public ResponseEntity<FitnessModel> addWorkoutsForUser(@PathVariable String userId, @RequestBody List<FitnessDto> workouts) {
        FitnessModel fitnessModel = fitnessService.addFitnessEntry(userId, workouts);
        return ResponseEntity.ok(fitnessModel);
    }

    @GetMapping("/workouts/user/{userId}")
    public ResponseEntity<FitnessModel> getFitnessDetailsByUserId(@PathVariable String userId) {
        return fitnessService.getFitnessDetailsByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



}
