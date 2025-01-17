package com.example.fitness_service.service;

import com.example.fitness_service.FitnessServiceApplication;
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
import java.time.temporal.ChronoUnit;
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

    public Mono<Void> addCustomWorkout(String userId, ExerciseDto customWorkout) {
        // Add the custom workout directly to the user's temporary workout list
        temporaryWorkoutList.computeIfAbsent(userId, k -> new ArrayList<>()).add(customWorkout);
        System.out.println("Custom Workout Added: " + customWorkout);
        return Mono.empty(); // No return value needed, just update the map
    }

    public Mono<Void> addWorkout(String userId, String workoutName, int inputReps, int inputSets, int inputDuration) {
        return fetchWorkoutDetails(workoutName)
                .doOnSuccess(baseWorkout -> {
                    // Dynamically calculate calories burned
                    float calculatedCalories = calculateCaloriesBurned(baseWorkout, inputReps, inputSets, inputDuration);

                    // Update the workout details
                    baseWorkout.setReps(inputReps);
                    baseWorkout.setSets(inputSets);
                    baseWorkout.setDuration(inputDuration);
                    baseWorkout.setCaloriesBurned(calculatedCalories);

                    // Add the workout to the user's temporary list
                    temporaryWorkoutList.computeIfAbsent(userId, k -> new ArrayList<>()).add(baseWorkout);
                    System.out.println("Workout Added: " + baseWorkout);
                })
                .then();
    }

    public Mono<FitnessModel> saveWorkoutAndCalculateCalories(String userId) {
        Mono<Boolean> hasWorkoutToday = fitnessRepository.findByUserIdAndFitnessDateBetween(userId, LocalDateTime.now().toLocalDate().atStartOfDay(), LocalDateTime.now()).hasElement();
        return hasWorkoutToday.flatMap(hasWorkout -> {
            if (hasWorkout) {
                return updateWorkout(userId);
            }
            else
            {
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


    // Update the user's workout for the current date by adding new workouts and recalculating the total calories burned
    public Mono<FitnessModel> updateWorkout(String userId) {
        // Get the current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Get the start of the day (00:00:00)
        LocalDateTime todayStart = currentDateTime.truncatedTo(ChronoUnit.DAYS);

        // Get the end of the day (23:59:59.999999999)
        LocalDateTime todayEnd = currentDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // First, check if a fitness entry for the user already exists for today
        return fitnessRepository.findByUserIdAndFitnessDateBetween(userId, todayStart, todayEnd)
                .flatMap(existingFitness -> {
                    // If a fitness entry already exists, add the new workouts from the temporary list
                    List<ExerciseDto> currentWorkoutList = existingFitness.getWorkoutList();
                    List<ExerciseDto> newWorkoutList = temporaryWorkoutList.getOrDefault(userId, new ArrayList<>());
                    currentWorkoutList.addAll(newWorkoutList); // Add new workouts to the existing list

                    // Recalculate total calories burned based on the updated workout list
                    float totalCaloriesBurned = (float) currentWorkoutList.stream()
                            .mapToDouble(ExerciseDto::getCaloriesBurned) // Calculate total calories for the workouts
                            .sum();

                    // Set the updated workout list and total calories burned
                    existingFitness.setWorkoutList(currentWorkoutList);
                    existingFitness.setTotalCaloriesBurned(totalCaloriesBurned);

                    // Save the updated FitnessModel
                    return fitnessRepository.save(existingFitness)
                            .doOnSuccess(savedFitness -> {
                                // Clear the temporary workout list after saving
                                temporaryWorkoutList.remove(userId);
                                System.out.println("Workout updated for user: " + userId);
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No fitness entry found for user on current date")));
    }


    public Mono<FitnessModel> editWorkout(String fitnessId, String workoutId, ExerciseDto updatedWorkout) {
        return fitnessRepository.findById(fitnessId)
                .flatMap(existingFitness -> {
                    Optional<ExerciseDto> workoutOpt = existingFitness.getWorkoutList().stream()
                            .filter(workout -> workout.getWorkoutId().equals(workoutId))
                            .findFirst();

                    if (workoutOpt.isPresent()) {
                        ExerciseDto existingWorkout = workoutOpt.get();

                        // Update fields
                        existingWorkout.setWorkoutName(updatedWorkout.getWorkoutName());
                        existingWorkout.setReps(updatedWorkout.getReps());
                        existingWorkout.setSets(updatedWorkout.getSets());
                        existingWorkout.setDuration(updatedWorkout.getDuration());

                        // Recalculate calories for the updated workout
                        float updatedCalories = calculateCaloriesBurned(existingWorkout,
                                updatedWorkout.getReps(),
                                updatedWorkout.getSets(),
                                updatedWorkout.getDuration());
                        existingWorkout.setCaloriesBurned(updatedCalories);

                        // Recalculate total calories for the fitness model
                        float totalCalories = existingFitness.getWorkoutList().stream()
                                .map(ExerciseDto::getCaloriesBurned)
                                .reduce(0.0f, Float::sum);

                        existingFitness.setTotalCaloriesBurned(totalCalories);

                        return fitnessRepository.save(existingFitness);
                    } else {
                        return Mono.error(new RuntimeException("Workout with ID " + workoutId + " not found"));
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Fitness record with ID " + fitnessId + " not found")));
    }

    public Mono<List<FitnessModel>> getWorkoutsByWeek(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return fitnessRepository.findAllByUserIdAndFitnessDateBetween(userId, startDate, endDate)
                .collectList()
                .switchIfEmpty(Mono.error(new RuntimeException("No fitness records found for the specified week")));
    }

    private float calculateCaloriesBurned(ExerciseDto baseWorkout, int inputReps, int inputSets, int inputDuration) {
        float baseCalories = baseWorkout.getCaloriesBurned();
        int baseReps = baseWorkout.getReps();
        int baseSets = baseWorkout.getSets();
        int baseDuration = baseWorkout.getDuration();

        float calculatedCalories = 0;

        // Contribution from duration
        if (baseDuration > 0) {
            calculatedCalories += baseCalories * ((float) inputDuration / baseDuration);
        }

        // Contribution from reps
        if (baseReps > 0) {
            calculatedCalories += baseCalories * 0.5f * ((float) inputReps / baseReps);
        }

        // Contribution from sets
        if (baseSets > 0) {
            calculatedCalories += baseCalories * 0.3f * ((float) inputSets / baseSets);
        }

        return calculatedCalories;
    }
    public Mono<FitnessModel> getWorkout(String userId) {
        // Define the start and end of today's date for the query
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // Find the existing FitnessModel entry for today and the given userId
        return fitnessRepository.findByUserIdAndFitnessDateBetween(userId, todayStart, todayEnd)
                .switchIfEmpty(Mono.error(new RuntimeException("No fitness record found for userId " + userId + " today")));
    }

}
