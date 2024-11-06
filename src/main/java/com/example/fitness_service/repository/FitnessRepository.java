package com.example.fitness_service.repository;

import com.example.fitness_service.model.FitnessModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FitnessRepository extends MongoRepository<FitnessModel, String> {
}
