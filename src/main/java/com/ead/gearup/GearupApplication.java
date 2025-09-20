package com.ead.gearup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GearupApplication {

	public static void main(String[] args) {
		System.out.println("URL: " + System.getenv("SPRING_DATASOURCE_URL"));
		System.out.println("User: " + System.getenv("SPRING_DATASOURCE_USERNAME"));

		SpringApplication.run(GearupApplication.class, args);

	}

}
