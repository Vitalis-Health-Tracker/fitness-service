package com.example.fitness_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FitnessServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitnessServiceApplication.class, args);
	}

	@Bean
	WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

}
