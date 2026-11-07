package com.sahil.project.StaySphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StaySphereApplication {

	public static void main(String[] args) {
		SpringApplication.run(StaySphereApplication.class, args);
	}

}
