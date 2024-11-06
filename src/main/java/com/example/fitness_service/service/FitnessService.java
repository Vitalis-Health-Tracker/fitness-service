package com.example.fitness_service.service;

import com.example.fitness_service.repository.FitnessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FitnessService {
    @Autowired
    FitnessRepository fitnessRepository;

}
