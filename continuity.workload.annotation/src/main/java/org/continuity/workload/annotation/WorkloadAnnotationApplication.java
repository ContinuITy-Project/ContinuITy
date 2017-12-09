package org.continuity.workload.annotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class WorkloadAnnotationApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkloadAnnotationApplication.class, args);
	}

}
