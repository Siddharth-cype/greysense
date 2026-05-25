package com.example.hexhiveint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the Hexive IoT Backend Service.
 *
 * <p>Bootstraps the Spring Boot application context, enabling JPA entity scanning
 * and repository auto-configuration for the {@code com.example.hexhiveint} package tree.</p>
 *
 * @author Hexive Team
 * @version 2.0
 */
@SpringBootApplication
@EntityScan("com.example.hexhiveint.model")
@EnableJpaRepositories("com.example.hexhiveint.repository")
public class HexhiveintApplication {

    /**
     * Launches the Hexive Spring Boot application.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(HexhiveintApplication.class, args);
    }
}
