package org.continuity.wessbas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class WessbasApplication {

	public static void main(String[] args) {
		SpringApplication.run(WessbasApplication.class, args);
	}
}
