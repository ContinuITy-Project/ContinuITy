package org.continuity.idpa.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class IdpaFromOldAnnotationConverter {

	private final IdpaYamlSerializer<Application> applicationSerializer = new IdpaYamlSerializer<>(Application.class);

	private final IdpaYamlSerializer<ApplicationAnnotation> annotationSerializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		IdpaFromOldAnnotationConverter converter = new IdpaFromOldAnnotationConverter();
		Path basePath = Paths.get("src", "main", "resources");

		converter.convertAnnotation(basePath.resolve("dvdstore-annotation-old.yml"), basePath.resolve("dvdstore-annotation.yml"));
		converter.convertSystemModel(basePath.resolve("dvdstore-systemmodel.yml"), basePath.resolve("dvdstore-application.yml"));
	}

	public void convertSystemModel(Path systemModelPath, Path applicationPath) throws JsonParseException, JsonMappingException, IOException {
		String systemModel = reduceLinesToString(Files.readAllLines(systemModelPath));
		Application converted = convertFromSystemModel(systemModel);
		applicationSerializer.writeToYaml(converted, applicationPath);
	}

	public void convertAnnotation(Path oldPath, Path newPath) throws JsonParseException, JsonMappingException, IOException {
		String annotation = reduceLinesToString(Files.readAllLines(oldPath));
		ApplicationAnnotation converted = convertFromAnnotation(annotation);
		annotationSerializer.writeToYaml(converted, newPath);
	}

	private String reduceLinesToString(List<String> lines) {
		StringBuilder builder = new StringBuilder();

		lines.forEach(l -> {
			builder.append(l);
			builder.append("\n");
		});

		return builder.toString();
	}

	public Application convertFromSystemModel(String systemModel) throws JsonParseException, JsonMappingException, IOException {
		systemModel = systemModel.replaceFirst("interfaces:", "endpoints:");

		return applicationSerializer.readFromYamlString(systemModel);
	}

	public ApplicationAnnotation convertFromAnnotation(String annotation) throws JsonParseException, JsonMappingException, IOException {
		annotation = annotation.replaceFirst("interface-annotations:", "endpoint-annotations:");
		annotation = annotation.replaceAll("- interface: ", "- endpoint: ");

		annotation = renameOverrides(annotation);

		return annotationSerializer.readFromYamlString(annotation);
	}

	private String renameOverrides(String annotation) {
		for (PropertyOverrideKey.HttpEndpoint key : PropertyOverrideKey.HttpEndpoint.values()) {
			String oldKey = "- HttpInterface." + key.name().toLowerCase() + ": ";
			String newKey = "- " + key.toString() + ": ";

			annotation = annotation.replaceAll(oldKey, newKey);
		}

		return annotation;
	}

}
