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
import java.util.List;


@Repository
public interface FitnessRepository extends ReactiveMongoRepository<FitnessModel, String> {
    Mono<FitnessModel> findByUserIdAndFitnessDate(String userId, LocalDate fitnessDate);
    Flux<FitnessModel> findByFitnessDate(LocalDate fitnessDate);
}
