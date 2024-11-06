package com.example.fitness_service.repository;

import com.example.fitness_service.model.fitnessModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface fitnessRepository extends MongoRepository<fitnessModel, String> {
}
