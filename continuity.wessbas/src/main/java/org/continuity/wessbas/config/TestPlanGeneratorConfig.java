package org.continuity.wessbas.config;

import org.continuity.wessbas.transform.jmeter.WessbasToJmeterConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class TestPlanGeneratorConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanGeneratorConfig.class);

	@Bean
	WessbasToJmeterConverter wessbasToJmeterConverter(@Value("${testplan.configuration.path:configuration}") String configurationPath) {
		LOGGER.info("Testplan configuration path is {}.", configurationPath);
		return new WessbasToJmeterConverter(configurationPath);
	}

}
