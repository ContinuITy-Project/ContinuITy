package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class SystemYamlTest {

	private static SystemModel systemModel = ContinuityModelTestInstance.SIMPLE.getSystemModel();

	@Test
	public void test() throws JsonGenerationException, JsonMappingException, IOException {
		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);
		serializer.writeToYaml(systemModel, "system-model.yml");
		SystemModel read = serializer.readFromYaml("system-model.yml");
		System.out.println(read);
	}

}
