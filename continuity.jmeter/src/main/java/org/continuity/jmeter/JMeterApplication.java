package org.continuity.jmeter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class JMeterApplication {

	public static void main(String[] args) {
		SpringApplication.run(JMeterApplication.class, args);
	}

}
