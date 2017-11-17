package org.continuity.wessbas.config;

import org.continuity.wessbas.transform.jmeter.WessbasToJmeterConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class TestPlanGeneratorConfig {

	@Bean
	WessbasToJmeterConverter wessbasToJmeterConverter(@Value("${testplan.configuration.path:configuration}") String configurationPath) {
		System.out.println("###\n###   Configuration Path is " + configurationPath + "\n###");
		return new WessbasToJmeterConverter(configurationPath);
	}

}
