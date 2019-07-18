package org.continuity.api.entities.artifact.markovbehavior;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MarkovChainSerializationTest {

	private ObjectMapper mapper;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
	}

	@Test
	public void testWriteRead() throws IOException {
		testWriteRead(MarkovChainTestInstance.SIMPLE);
		testWriteRead(MarkovChainTestInstance.SIMPLE_WO_A);
		testWriteRead(MarkovChainTestInstance.SIMPLE_INSERT);
		testWriteRead(MarkovChainTestInstance.SIMPLE_WITH_INSERT);
		testWriteRead(MarkovChainTestInstance.SOCK_SHOP);
	}

	private void testWriteRead(MarkovChainTestInstance instance) throws IOException {
		String[][] csv = instance.getCsv();

		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(csv);
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chain);

		RelativeMarkovChain parsed = mapper.readValue(json, RelativeMarkovChain.class);

		assertThat(parsed.getTransitions().toString()).isEqualTo(chain.getTransitions().toString());
		assertThat(parsed.getTransitions().getClass()).isEqualTo(chain.getTransitions().getClass()).as("The map type should be TreeMap.");
		assertThat(parsed.getTransitions().values()).extracting(Map::getClass).extracting(Class.class::cast).containsOnly(TreeMap.class).as("The type of the inner maps should be TreeMap.");
	}

	@Test
	public void testEmptyWriteRead() throws IOException {
		testEmptyWriteRead(MarkovChainTestInstance.SIMPLE);
		testEmptyWriteRead(MarkovChainTestInstance.SIMPLE_WO_A);
		testEmptyWriteRead(MarkovChainTestInstance.SIMPLE_INSERT);
		testEmptyWriteRead(MarkovChainTestInstance.SIMPLE_WITH_INSERT);
		testEmptyWriteRead(MarkovChainTestInstance.SOCK_SHOP);
	}

	private void testEmptyWriteRead(MarkovChainTestInstance instance) throws IOException {
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(instance.getCsv());

		for (String state : chain.getRequestStates()) {
			chain.removeState(state, NormalDistribution.ZERO);
		}

		NormalDistribution origThinkTime = chain.getTransition(RelativeMarkovChain.INITIAL_STATE, RelativeMarkovChain.FINAL_STATE).getThinkTime();

		assertThat(origThinkTime.getMean()).isGreaterThan(0);

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chain);
		RelativeMarkovChain parsed = mapper.readValue(json, RelativeMarkovChain.class);
		RelativeMarkovTransition parsedTransition = parsed.getTransition(RelativeMarkovChain.INITIAL_STATE, RelativeMarkovChain.FINAL_STATE);

		assertThat(parsedTransition.getProbability()).isEqualTo(1.0, Offset.offset(0.001));
		assertThat(parsedTransition.getThinkTime().getMean()).isEqualTo(origThinkTime.getMean(), Offset.offset(0.01));
		assertThat(parsedTransition.getThinkTime().getVariance()).isEqualTo(origThinkTime.getVariance(), Offset.offset(0.01));
	}

}
