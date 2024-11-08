package com.example.fitness_service.service;

import com.example.fitness_service.dto.FitnessDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.repository.FitnessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
public class FitnessService {
    private final FitnessRepository fitnessRepository;
    private final WebClient webClient;

    public FitnessService(FitnessRepository fitnessRepository, WebClient.Builder webClientBuilder) {
        this.fitnessRepository = fitnessRepository;
        this.webClient = webClientBuilder.baseUrl("http://workout-service/api").build();
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

    public Mono<Float> calculateTotalCalories(String userId, LocalDate fitnessDate) {
        return fitnessRepository.findByUserIdAndFitnessDate(userId, fitnessDate)
                .flatMapMany(fitness -> Flux.fromIterable(fitness.getWorkoutList()))
                .map(FitnessDto::getCaloriesBurned)
                .reduce(0.0f, Float::sum);
    }

    public Mono<FitnessModel> updateWorkout(String fitnessId, FitnessDto updatedWorkout) {
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

    private Mono<FitnessDto> fetchWorkoutDetails(String workoutName) {
        return webClient.get()
                .uri("/workouts/{name}", workoutName)
                .retrieve()
                .bodyToMono(FitnessDto.class);
    }
}
