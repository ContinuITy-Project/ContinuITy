package org.continuity.idpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class IdpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdpaApplication.class, args);
	}

}
