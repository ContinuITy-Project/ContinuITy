package org.continuity.cli.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class ArtifactCommands {

	@Autowired
	private RestTemplate restTemplate;

	private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));

	@ShellMethod(key = { "get" }, value = "Gets an artifact.")
	public String get(String link) throws JsonProcessingException {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity(link, JsonNode.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "Could not access the link! Response code is " + response.getStatusCode();
		}

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody());
	}

}
