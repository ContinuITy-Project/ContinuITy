package org.continuity.request.rates.config;

import org.continuity.commons.jmeter.TestPlanWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JMeterConfig {

	@Bean
	public TestPlanWriter testPlanWriter() {
		return new TestPlanWriter("../");
	}

}
