

package com.example.hexhiveint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.hexhiveint.model")
@EnableJpaRepositories("com.example.hexhiveint.repository")
public class HexhiveintApplication {

	public static void main(String[] args) {
		System.out.println("==========================================");
		System.out.println("   STARTING HEXHIVEINT WITH DEBUGGING");
		System.out.println("   GLOBAL EXCEPTION HANDLER ACTIVE");
		System.out.println("==========================================");
		SpringApplication.run(HexhiveintApplication.class, args);
	}

}
