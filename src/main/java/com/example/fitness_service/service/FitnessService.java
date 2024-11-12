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
import java.util.*;

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


    public Mono<FitnessModel> deleteWorkout(String userId, String workoutId) {
        // Define the start and end of today's date for the query
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // Find the existing FitnessModel entry for today and the given userId
        return fitnessRepository.findByUserIdAndFitnessDateBetween(userId, todayStart, todayEnd)
                .flatMap(existingFitness -> {
                    // Find and remove the workout from the existing workout list
                    boolean workoutRemoved = existingFitness.getWorkoutList().removeIf(workout -> workout.getWorkoutId().equals(workoutId));

                    if (workoutRemoved) {
                        // Recalculate total calories after removal
                        float totalCalories = existingFitness.getWorkoutList().stream()
                                .map(ExerciseDto::getCaloriesBurned)
                                .reduce(0.0f, Float::sum);

                        existingFitness.setTotalCaloriesBurned(totalCalories); // Update total calories burned

                        // Save the updated fitness record
                        return fitnessRepository.save(existingFitness);
                    } else {
                        // If the workout was not found, return an error
                        return Mono.error(new RuntimeException("Workout with ID " + workoutId + " not found"));
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No fitness record found for userId " + userId + " today")));
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


    public Mono<FitnessModel> updateWorkoutList(String userId, String workoutName) {
        return fetchWorkoutDetails(workoutName)
                .flatMap(workout -> {
                    // Find the user's fitness record for today
                    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
                    LocalDateTime endOfDay = startOfDay.plusDays(1);

                    return fitnessRepository.findByUserIdAndFitnessDateBetween(userId, startOfDay, endOfDay)
                            .flatMap(existingFitness -> {
                                // Add the fetched workout to the list
                                existingFitness.getWorkoutList().add(workout);

                                // Recalculate total calories burned
                                float totalCalories = existingFitness.getWorkoutList().stream()
                                        .map(ExerciseDto::getCaloriesBurned)
                                        .reduce(0.0f, Float::sum);

                                existingFitness.setTotalCaloriesBurned(totalCalories);

                                // Save the updated fitness record
                                return fitnessRepository.save(existingFitness);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // If the fitness record doesn't exist, create a new one
                                FitnessModel newFitnessModel = new FitnessModel();
                                newFitnessModel.setUserId(userId);
                                newFitnessModel.setFitnessDate(LocalDateTime.now());
                                newFitnessModel.setWorkoutList(Collections.singletonList(workout));

                                // Set total calories (in case of first workout)
                                newFitnessModel.setTotalCaloriesBurned(workout.getCaloriesBurned());

                                return fitnessRepository.save(newFitnessModel);
                            }));
                });
    }


    public Mono<FitnessModel> editWorkout(String fitnessId, String workoutId, ExerciseDto updatedWorkout) {
        return fitnessRepository.findById(fitnessId)
                .flatMap(existingFitness -> {
                    // Find the workout to update in the existing workout list
                    Optional<ExerciseDto> workoutOpt = existingFitness.getWorkoutList().stream()
                            .filter(workout -> workout.getWorkoutId().equals(workoutId))
                            .findFirst();

                    // If the workout is found, update it
                    if (workoutOpt.isPresent()) {
                        ExerciseDto existingWorkout = workoutOpt.get();
                        // Update fields
                        existingWorkout.setWorkoutName(updatedWorkout.getWorkoutName());
                        existingWorkout.setReps(updatedWorkout.getReps());
                        existingWorkout.setSets(updatedWorkout.getSets());
                        existingWorkout.setDuration(updatedWorkout.getDuration());
                        existingWorkout.setCaloriesBurned(updatedWorkout.getCaloriesBurned());
                        existingWorkout.setWorkoutType(updatedWorkout.getWorkoutType());

                        // Recalculate total calories after update
                        float totalCalories = existingFitness.getWorkoutList().stream()
                                .map(ExerciseDto::getCaloriesBurned)
                                .reduce(0.0f, Float::sum);

                        existingFitness.setTotalCaloriesBurned(totalCalories);
                        existingFitness.setFitnessId(fitnessId);

                        // Save the updated document
                        return fitnessRepository.save(existingFitness);
                    } else {
                        // If no workout is found, return an error
                        return Mono.error(new RuntimeException("Workout with ID " + workoutId + " not found"));
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Fitness record with ID " + fitnessId + " not found")));
    }




}
