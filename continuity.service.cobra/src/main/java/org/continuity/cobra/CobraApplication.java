package org.continuity.cobra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Alper Hidiroglu
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class CobraApplication {

	public static void main(String[] args) {
		SpringApplication.run(CobraApplication.class, args);
	}

}