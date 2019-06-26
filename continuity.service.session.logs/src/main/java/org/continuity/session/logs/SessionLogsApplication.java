package org.continuity.session.logs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Alper Hidiroglu
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class SessionLogsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SessionLogsApplication.class, args);
	}

}