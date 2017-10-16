package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.workload.dsl.system.TargetSystem;
import org.continuity.workload.dsl.yaml.ContinuityYamlSerializer;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class SystemYamlTest {

	private static TargetSystem systemModel = ContinuityModelTestInstance.SIMPLE.getSystemModel();

	@Test
	public void test() throws JsonGenerationException, JsonMappingException, IOException {
		ContinuityYamlSerializer<TargetSystem> serializer = new ContinuityYamlSerializer<>(TargetSystem.class);
		serializer.writeToYaml(systemModel, "system-model.yml");
		TargetSystem read = serializer.readFromYaml("system-model.yml");
		System.out.println(read);
	}

}
