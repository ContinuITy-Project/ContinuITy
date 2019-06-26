package org.continuity.request.rates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class RequestRatesApplication {

	public static void main(String[] args) {
		SpringApplication.run(RequestRatesApplication.class, args);
	}
}
