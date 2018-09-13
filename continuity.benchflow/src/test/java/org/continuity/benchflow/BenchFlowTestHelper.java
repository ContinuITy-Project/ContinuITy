package org.continuity.benchflow;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.yaml.IdpaYamlSerializer;
import org.testng.Assert;
import org.testng.reporters.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BenchFlowTestHelper {

	public static final String TEST_IDPA_ANNOTATION_FILE_1 = "1_annotation_model_1.yaml";
	public static final String TEST_IDPA_APPLICATION_FILE_1 = "1_application_model_1.yaml";
	public static final String TEST_IDPA_APPLICATION_FILE_2 = "1_application_model_2.yaml";
	public static final String TEST_BEHAVIOR_FILE_1 = "1_behavior_model.json";
	
	public <T extends IdpaElement> T getIdpaModelFromFile(final Class<T> type, final String filename) {
		String fileContent = getFileContent(filename);
		return getIdpaModelFromString(type, fileContent);
	}		
	
	private <T extends IdpaElement> T getIdpaModelFromString(final Class<T> type, final String model) {
		IdpaYamlSerializer<T> serializer = new IdpaYamlSerializer<>(type);
		try {
			return serializer.readFromYamlString(model);
		} catch (IOException e) {
			Assert.fail("Exception during parsing IDPA model! Message: " + e.getMessage());
			return null;
		}
	}	
	
	private String getFileContent(final String filename) {
	    URL resource = getClass().getClassLoader().getResource(filename);
	    File file = new File(resource.getFile());
	    try {
			return Files.readFile(file);
		} catch (IOException e) {
			Assert.fail("Exception during reading test file! Message: " + e.getMessage());
			return null;
		}
	}
	
	public BehaviorModel getBehaviorModelFromFile(final String filename) {
		String behavior = getFileContent(filename);
				
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(behavior, BehaviorModel.class);
		} catch (IOException e) {
			Assert.fail("Exception during parsing behavior model! Message: " + e.getMessage());
			return null;
		}
	}	
}
