package org.continuity.commons.utils;

import java.io.IOException;

import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Can be used to remove all parameters from an application model (helpful, if the parameters were
 * transformed from a Swagger specification but never used).
 *
 * @author Henning Schulz
 *
 */
public class RemoveParametersFromApplication {

	private final String inputFile;

	public RemoveParametersFromApplication(String inputFile) {
		this.inputFile = inputFile;
	}

	public void removeParameters(String[] args) throws JsonParseException, JsonMappingException, IOException {
		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
		Application application = serializer.readFromYaml(inputFile);

		IdpaByClassSearcher<HttpEndpoint> interfaceSearcher = new IdpaByClassSearcher<>(HttpEndpoint.class, this::removeParameters);
		interfaceSearcher.visit(application);

		String newFile = inputFile.substring(0, inputFile.length() - 4) + "-wo-params.yml";
		serializer.writeToYaml(application, newFile);
	}

	private void removeParameters(HttpEndpoint interf) {
		interf.getParameters().clear();
	}

}
