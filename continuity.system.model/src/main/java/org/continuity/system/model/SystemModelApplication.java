package org.continuity.system.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class SystemModelApplication {

	public static void main(String[] args) {
		SpringApplication.run(SystemModelApplication.class, args);
	}

}
