package org.continuity.benchflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Manuel Palenga
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class BenchFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(BenchFlowApplication.class, args);
	}

}
