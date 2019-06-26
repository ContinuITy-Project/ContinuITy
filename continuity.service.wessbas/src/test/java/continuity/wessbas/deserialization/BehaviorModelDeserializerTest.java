package continuity.wessbas.deserialization;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.deserialization.BehaviorModelSerializer;
import org.junit.Before;
import org.junit.Test;

import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * Tests {@link DeserializeBehaviorModel}
 * 
 * @author Tobias Angerstein
 *
 */
public class BehaviorModelDeserializerTest {

	/**
	 * Sample CSV string
	 */
	protected static final String CSV_FILE_PATH = "src/test/resources/gen_behavior_model0.csv";

	/**
	 * Tests {@link BehaviorModelSerializer#deserializeBehaviorModel(String[][])}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	public static class DeserializeBehaviorModel extends BehaviorModelDeserializerTest {
		
		private CSVHandler csvHandler;

		/**
		 * Initialize necessary objects
		 */
		@Before
		public void initialize() {
			csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);
		}
		
		@Test
		public void deserializeValidCSV() throws FileNotFoundException, NullPointerException, IOException {
			String[][] behaviorModelArray = csvHandler.readValues(CSV_FILE_PATH);
			Behavior deserializedModel = BehaviorModelSerializer.deserializeBehaviorModel(behaviorModelArray);
		}
	}
}
