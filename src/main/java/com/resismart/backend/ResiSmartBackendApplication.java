package com.resismart.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResiSmartBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResiSmartBackendApplication.class, args);
	}

}
