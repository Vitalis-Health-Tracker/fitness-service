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
import java.util.ArrayList;
import java.util.List;

@Service
public class FitnessService {
    private final FitnessRepository fitnessRepository;
    private final WebClient webClient;


    public FitnessService(FitnessRepository fitnessRepository, WebClient.Builder webClientBuilder) {
        this.fitnessRepository = fitnessRepository;
        this.webClient = webClientBuilder.baseUrl("https://sharunraj.github.io/fitnessApi.github.io/").build();
    }

    public Mono<FitnessModel> addWorkout(String userId, String workoutName) {
        return fetchWorkoutDetails(workoutName)
                .flatMap(workout -> fitnessRepository.findByUserIdAndFitnessDate(userId, LocalDate.now())
                        .switchIfEmpty(Mono.defer(() -> {
                            FitnessModel newFitness = new FitnessModel();
                            newFitness.setUserId(userId);
                            newFitness.setFitnessDate(LocalDate.now());
                            newFitness.setWorkoutList(List.of(workout));
                            return fitnessRepository.save(newFitness);
                        }))
                        .flatMap(fitness -> {
                            fitness.getWorkoutList().add(workout);
                            return fitnessRepository.save(fitness);
                        }));
    }

    public Mono<FitnessModel> addWorkout2(String workoutName) {
        return fetchWorkoutDetails(workoutName)
                .flatMap(workout -> fitnessRepository.findByFitnessDate(LocalDate.now())
                        .singleOrEmpty()
                        .flatMap(fitness -> {

                            fitness.getWorkoutList().add(workout);
                            return fitnessRepository.save(fitness);
                        })
                        .switchIfEmpty(Mono.defer(() -> {

                            FitnessModel newFitness = new FitnessModel();
                            newFitness.setFitnessDate(LocalDate.now());
                            newFitness.setWorkoutList(List.of(workout));
                            return fitnessRepository.save(newFitness);
                        }))
                );
    }




    public Mono<Float> calculateTotalCalories(String userId, LocalDate fitnessDate) {
        return fitnessRepository.findByUserIdAndFitnessDate(userId, fitnessDate)
                .flatMapMany(fitness -> Flux.fromIterable(fitness.getWorkoutList()))
                .map(ExerciseDto::getCaloriesBurned)
                .reduce(0.0f, Float::sum);
    }

    public Mono<FitnessModel> updateWorkout(String fitnessId, ExerciseDto updatedWorkout) {
        return fitnessRepository.findById(fitnessId)
                .flatMap(fitness -> {
                    fitness.getWorkoutList().stream()
                            .filter(w -> w.getWorkoutId().equals(updatedWorkout.getWorkoutId()))
                            .findFirst()
                            .ifPresent(existingWorkout -> {
                                existingWorkout.setWorkoutName(updatedWorkout.getWorkoutName());
                                existingWorkout.setReps(updatedWorkout.getReps());
                                existingWorkout.setSets(updatedWorkout.getSets());
                                existingWorkout.setDuration(updatedWorkout.getDuration());
                                existingWorkout.setWorkoutType(updatedWorkout.getWorkoutType());
                                existingWorkout.setCaloriesBurned(updatedWorkout.getCaloriesBurned());
                            });
                    return fitnessRepository.save(fitness);
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
