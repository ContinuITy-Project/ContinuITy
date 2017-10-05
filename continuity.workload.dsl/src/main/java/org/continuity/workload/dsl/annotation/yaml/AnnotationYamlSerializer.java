package org.continuity.workload.dsl.annotation.yaml;

import java.io.File;
import java.io.IOException;

import org.continuity.workload.dsl.annotation.SystemAnnotation;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationYamlSerializer {

	public SystemAnnotation readFromYaml(String yamlSource) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES));
		return mapper.readValue(yamlSource, SystemAnnotation.class);
	}

	public void writeToYaml(SystemAnnotation annotation, String yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES));
		mapper.writeValue(new File(yamlFile), annotation);
	}

}
