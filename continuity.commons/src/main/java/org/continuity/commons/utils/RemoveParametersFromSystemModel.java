package org.continuity.commons.utils;

import java.io.IOException;

import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Can be used to remove all parameters from a system model (helpful, if the parameters were
 * transformed from a Swagger specification but never used).
 *
 * @author Henning Schulz
 *
 */
public class RemoveParametersFromSystemModel {

	private final String inputFile;

	public RemoveParametersFromSystemModel(String inputFile) {
		this.inputFile = inputFile;
	}

	public void removeParameters(String[] args) throws JsonParseException, JsonMappingException, IOException {
		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);
		SystemModel system = serializer.readFromYaml(inputFile);

		ContinuityByClassSearcher<HttpInterface> interfaceSearcher = new ContinuityByClassSearcher<>(HttpInterface.class, this::removeParameters);
		interfaceSearcher.visit(system);

		String newFile = inputFile.substring(0, inputFile.length() - 4) + "-wo-params.yml";
		serializer.writeToYaml(system, newFile);
	}

	private void removeParameters(HttpInterface interf) {
		interf.getParameters().clear();
	}

}
