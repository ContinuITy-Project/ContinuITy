package continuity.idpa;

import static org.junit.Assert.assertNotNull;

import org.continuity.idpa.test.IdpaTestInstance;
import org.junit.Test;

public class IdpaTestInstanceTest {

	@Test
	public void test() {
		for (IdpaTestInstance instance : IdpaTestInstance.values()) {
			assertNotNull("Application model " + instance + " need not be null!", instance.getApplication());
			assertNotNull("Annotation model " + instance + " need not be null!", instance.getAnnotation());
		}
	}

}
