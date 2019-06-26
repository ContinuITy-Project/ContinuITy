package org.continuity.wessbas.config;

import org.continuity.wessbas.transform.benchflow.WessbasToBehaviorModelConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author Manuel Palenga
 *
 */
@Configuration
public class BehaviorModelConverterConfig {

	@Bean
	WessbasToBehaviorModelConverter wessbasToBehaviorModelConverter() {
		return new WessbasToBehaviorModelConverter();
	}

}
