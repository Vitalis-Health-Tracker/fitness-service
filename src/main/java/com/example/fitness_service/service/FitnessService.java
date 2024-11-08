package com.example.fitness_service.service;

import com.example.fitness_service.dto.FitnessDto;
import com.example.fitness_service.model.FitnessModel;
import com.example.fitness_service.repository.FitnessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
public class FitnessService {
    private final FitnessRepository fitnessRepository;
    private final WebClient webClient;

    @Autowired
    public FitnessService(FitnessRepository fitnessRepository, WebClient.Builder webClientBuilder) {
        this.fitnessRepository = fitnessRepository;
        this.webClient = webClientBuilder.baseUrl("https://externalapi.com").build();
    }

    public Mono<FitnessDto> getWorkoutDetails(String workoutName) {
        return webClient.get()
                .uri("/workouts/" + workoutName)
                .retrieve()
                .bodyToMono(FitnessDto.class);
    }

    private float calculateCalories(FitnessDto workout) {
        return workout.getReps() * workout.getSets() * workout.getDuration() * 0.1f;
    }

    public Mono<FitnessModel> addFitnessEntry(String userId, List<FitnessDto> workouts) {
        float totalCalories = 0;
        for (FitnessDto workout : workouts) {
            float calories = calculateCalories(workout);
            workout.setCaloriesBurned(calories);
            totalCalories += calories;
        }

        FitnessModel fitnessModel = new FitnessModel();
        fitnessModel.setUserId(userId);
        fitnessModel.setFitnessDate(LocalDate.now());
        fitnessModel.setWorkoutList(workouts);

        return fitnessRepository.save(fitnessModel);
    }

    public List<FitnessModel> getFitnessDetailsByUserId(String userId) {
        return fitnessRepository.findByUserId(userId);
    }
}
