package com.example.fitness_service.repository;

import com.example.fitness_service.model.FitnessModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface FitnessRepository extends ReactiveMongoRepository<FitnessModel, String> {
    List<FitnessModel> findByUserId(String userId);
}
