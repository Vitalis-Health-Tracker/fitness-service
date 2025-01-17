package com.example.fitness_service.repository;

import com.example.fitness_service.model.FitnessModel;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface FitnessRepository extends ReactiveMongoRepository<FitnessModel, String> {
    Mono<FitnessModel> findByUserIdAndFitnessDate(String userId, LocalDateTime fitnessDate);

    Mono<FitnessModel> findByUserIdAndFitnessDateBetween(String userId, LocalDateTime todayStart, LocalDateTime todayEnd);

    Flux<FitnessModel> findAllByUserIdAndFitnessDateBetween(String userId, LocalDateTime start, LocalDateTime end);
    Mono<FitnessModel> findByUserId(String userId);
}
