package com.selimhorri.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@EnableEurekaClient
@EnableCaching
public class FeatureToggleServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeatureToggleServiceApplication.class, args);
	}

}
