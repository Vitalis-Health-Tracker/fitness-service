package com.example.fitness_service.service;

import com.example.fitness_service.dto.ExerciseDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.repository.FitnessRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FitnessService {
    private final FitnessRepository fitnessRepository;
    private final WebClient webClient;
    private final Map<String, List<ExerciseDto>> temporaryWorkoutList = new HashMap<>();


    public FitnessService(FitnessRepository fitnessRepository, WebClient.Builder webClientBuilder) {
        this.fitnessRepository = fitnessRepository;
        this.webClient = webClientBuilder.baseUrl("https://sharunraj.github.io/fitnessApi.github.io/").build();
    }

    public Mono<Void> addWorkout(String userId, String workoutName) {
        return fetchWorkoutDetails(workoutName)
                .doOnSuccess(workout -> {
                    // Temporary store in the map by userId
                    //Display the workout details
                    temporaryWorkoutList.computeIfAbsent(userId, k -> new ArrayList<>()).add(workout);
                    System.out.println("Workout Details: " + workout);
                })
                .then();

    }

    public Mono<FitnessModel> saveWorkoutAndCalculateCalories(String userId) {
        // Get the workout list for the user
        List<ExerciseDto> workoutList = temporaryWorkoutList.getOrDefault(userId, new ArrayList<>());

        // Calculate the total calories from the workout list in a non-blocking way
        Mono<Float> totalCaloriesMono = Flux.fromIterable(workoutList)
                .map(ExerciseDto::getCaloriesBurned)
                .reduce(0.0f, Float::sum); // No blocking here

        // Create a new FitnessModel object after the total calories are calculated
        return totalCaloriesMono.flatMap(totalCalories -> {
            FitnessModel fitnessModel = new FitnessModel();
            fitnessModel.setUserId(userId);
            fitnessModel.setFitnessDate(LocalDateTime.now()); // Set the current date and time
            fitnessModel.setWorkoutList(workoutList);
            fitnessModel.setTotalCaloriesBurned(totalCalories); // Set the total calories burned

            // Save the FitnessModel with the total calories
            return fitnessRepository.save(fitnessModel)
                    .doOnSuccess(savedFitness -> temporaryWorkoutList.remove(userId)); // Clear the temporary list after saving
        });
    }

    public Mono<FitnessModel> updateWorkoutAndCalculateCalories(String userId) {
        // Get the workout list for the user
        List<ExerciseDto> workoutList = temporaryWorkoutList.getOrDefault(userId, new ArrayList<>());

        // Calculate the total calories from the workout list in a non-blocking way
        Mono<Float> totalCaloriesMono = Flux.fromIterable(workoutList)
                .map(ExerciseDto::getCaloriesBurned)
                .reduce(0.0f, Float::sum); // No blocking here

        return totalCaloriesMono.flatMap(totalCalories -> {
            // Search for an existing FitnessModel by userId and today's date
            LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay(); // Start of today's date
            return fitnessRepository.findByUserIdAndFitnessDateBetween(userId, today, today.plusDays(1))
                    .flatMap(existingFitness -> {
                        // Update the existing record if found
                        existingFitness.setWorkoutList(workoutList);
                        existingFitness.setTotalCaloriesBurned(totalCalories);
                        existingFitness.setFitnessDate(today); // Ensure the fitness date is today's date

                        // Save the updated record
                        return fitnessRepository.save(existingFitness)
                                .doOnSuccess(savedFitness -> temporaryWorkoutList.remove(userId)); // Clear the temporary list after saving
                    })
                    .switchIfEmpty(
                            // If no record found, create a new one
                            Mono.defer(() -> {
                                FitnessModel newFitnessModel = new FitnessModel();
                                newFitnessModel.setUserId(userId);
                                newFitnessModel.setFitnessDate(today); // Set the current date
                                newFitnessModel.setWorkoutList(workoutList);
                                newFitnessModel.setTotalCaloriesBurned(totalCalories); // Set the total calories burned

                                // Save the new record
                                return fitnessRepository.save(newFitnessModel)
                                        .doOnSuccess(savedFitness -> temporaryWorkoutList.remove(userId)); // Clear the temporary list after saving
                            })
                    );
        });
    }



    public Mono<Void> deleteWorkout(String fitnessId, String workoutId) {
        return fitnessRepository.findById(fitnessId)
                .flatMap(fitness -> {
                    fitness.getWorkoutList().removeIf(workout -> workout.getWorkoutId().equals(workoutId));
                    return fitnessRepository.save(fitness);
                })
                .then();
    }

    private Mono<ExerciseDto> fetchWorkoutDetails(String workoutName) {
        return webClient.get()
                .uri("https://sharunraj.github.io/fitnessApi.github.io/FitnessAPI.json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> Mono.error(new RuntimeException("API request failed")))
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    for (JsonNode workout : jsonNode) {
                        if (workout.get("workoutName").asText().equalsIgnoreCase(workoutName)) {
                            return new ExerciseDto(
                                    workout.get("workoutId").asText(),
                                    workout.get("workoutName").asText(),
                                    workout.get("reps").asInt(),
                                    workout.get("sets").asInt(),
                                    workout.get("duration").asInt(),
                                    workout.get("workoutType").asText(),
                                    workout.get("caloriesBurned").floatValue()
                            );
                        }
                    }
                    return null;
                });
    }

}
