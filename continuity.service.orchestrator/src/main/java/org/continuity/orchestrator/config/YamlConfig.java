package org.continuity.orchestrator.config;

import java.util.List;

import org.continuity.api.entities.deserialization.YamlJackson2HttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

@Configuration
public class YamlConfig implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		YamlJackson2HttpMessageConverter converter = new YamlJackson2HttpMessageConverter();
		converter.getObjectMapper().registerModule(new Jdk8Module());
		converters.add(converter);
	}

}
