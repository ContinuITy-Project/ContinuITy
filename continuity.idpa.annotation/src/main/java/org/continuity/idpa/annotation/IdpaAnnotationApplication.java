package org.continuity.idpa.annotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class IdpaAnnotationApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdpaAnnotationApplication.class, args);
	}

}
