package org.continuity.api.entities.deserialization;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

public class YamlJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	public YamlJackson2HttpMessageConverter() {
		super(new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).disable(Feature.USE_NATIVE_TYPE_ID)), MediaType.parseMediaType("application/x-yaml"));
	}

}
