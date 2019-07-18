package org.continuity.api.entities.artifact.markovbehavior;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.assertj.core.data.Offset;
import org.junit.Test;

public class MarkovChainTest {

	@Test
	public void testReadWrite() throws IOException {
		testReadWrite(MarkovChainTestInstance.SOCK_SHOP);
		testReadWrite(MarkovChainTestInstance.SIMPLE);
		testReadWrite(MarkovChainTestInstance.SIMPLE_WO_A);
		testReadWrite(MarkovChainTestInstance.SIMPLE_INSERT);
		testReadWrite(MarkovChainTestInstance.SIMPLE_WITH_INSERT);
	}

	@Test
	public void testSpecialNumbers() throws IOException {
		String[][] csv = MarkovChainTestInstance.SPECIAL_NUMBERS.getCsv();
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(csv);
		System.out.println(chain);
	}

	private void testReadWrite(MarkovChainTestInstance instance) throws IOException {
		String[][] csv = instance.getCsv();
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(csv);

		assertThat(chain.toCsv()).isEqualTo(csv);
	}

	@Test
	public void testRemoveState() throws IOException {
		String[][] origCsv = MarkovChainTestInstance.SIMPLE.getCsv();
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(origCsv);

		chain.removeState("a", NormalDistribution.ZERO);

		String[][] newCsv = MarkovChainTestInstance.SIMPLE_WO_A.getCsv();

		assertThat(chain.toCsv()).isEqualTo(newCsv);
	}

	@Test
	public void testRenameState() throws IOException {
		testRenameState(MarkovChainTestInstance.SIMPLE, "a");
		testRenameState(MarkovChainTestInstance.SIMPLE, "b");
		testRenameState(MarkovChainTestInstance.SIMPLE, "c");
		testRenameState(MarkovChainTestInstance.SOCK_SHOP, "cartUsingGET");
		testRenameState(MarkovChainTestInstance.SOCK_SHOP, "getCatalogueItemUsingGET");
		testRenameState(MarkovChainTestInstance.SOCK_SHOP, "loginUsingGET");

		testRenameState(MarkovChainTestInstance.SIMPLE, RelativeMarkovChain.INITIAL_STATE);
		testRenameState(MarkovChainTestInstance.SIMPLE, RelativeMarkovChain.FINAL_STATE);
		testRenameState(MarkovChainTestInstance.SOCK_SHOP, RelativeMarkovChain.INITIAL_STATE);
		testRenameState(MarkovChainTestInstance.SOCK_SHOP, RelativeMarkovChain.FINAL_STATE);
	}

	private void testRenameState(MarkovChainTestInstance instance, String state) throws IOException {
		String[][] csv = instance.getCsv();
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(csv);

		chain.renameState(state, state + "2");
		chain.renameState(state + "2", state);

		assertThat(chain.toCsv()).isEqualTo(csv);
	}

	@Test
	public void testAddSubChain() throws IOException {
		String[][] origCsv = MarkovChainTestInstance.SIMPLE.getCsv();
		String[][] insertCsv = MarkovChainTestInstance.SIMPLE_INSERT.getCsv();
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(origCsv);
		chain.setId("orig");
		RelativeMarkovChain insert = RelativeMarkovChain.fromCsv(insertCsv);
		insert.setId("insert");

		chain.replaceState("a", insert);

		String[][] newCsv = MarkovChainTestInstance.SIMPLE_WITH_INSERT.getCsv();

		assertThat(chain.toCsv()).isEqualTo(newCsv);
	}

	@Test
	public void testThinkTimeAfterStateRemoval() throws IOException {
		testThinkTimeAfterStateRemovals(MarkovChainTestInstance.SIMPLE, 34.833);
		testThinkTimeAfterStateRemovals(MarkovChainTestInstance.SOCK_SHOP, 654368.40);
	}

	private void testThinkTimeAfterStateRemovals(MarkovChainTestInstance instance, double expectedThinkTime) throws IOException {
		String[][] csv = instance.getCsv();
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(csv);

		for (String state : chain.getRequestStates()) {
			chain.removeState(state, NormalDistribution.ZERO);
		}

		assertThat(chain.getRequestStates()).isEmpty();
		assertThat(chain.getTransition(RelativeMarkovChain.INITIAL_STATE, RelativeMarkovChain.FINAL_STATE).getThinkTime().getMean()).isEqualTo(expectedThinkTime, Offset.offset(0.01));
	}

	@Test
	public void testThinkTimeWithSubChain() throws IOException {
		testThinkTimeWithSubChain(MarkovChainTestInstance.SIMPLE, MarkovChainTestInstance.SIMPLE_INSERT, "a");
		testThinkTimeWithSubChain(MarkovChainTestInstance.SOCK_SHOP, MarkovChainTestInstance.SIMPLE, "cartUsingGET");
	}

	private void testThinkTimeWithSubChain(MarkovChainTestInstance origInstance, MarkovChainTestInstance insertInstance, String state) throws IOException {
		String[][] origCsv = origInstance.getCsv();
		String[][] insertCsv = insertInstance.getCsv();

		RelativeMarkovChain insert = RelativeMarkovChain.fromCsv(insertCsv);
		NormalDistribution insertDuration = calculateDuration(insert);
		RelativeMarkovChain chain = RelativeMarkovChain.fromCsv(origCsv);
		chain.removeState(state, insertDuration);
		NormalDistribution origDuration = calculateDuration(chain);

		chain = RelativeMarkovChain.fromCsv(origCsv);
		chain.setId("orig");
		insert = RelativeMarkovChain.fromCsv(insertCsv);
		insert.setId("insert");

		chain.replaceState(state, insert);
		NormalDistribution duration = calculateDuration(chain);

		assertThat(duration.getMean()).as("mean").isEqualTo(origDuration.getMean(), Offset.offset(0.01));
		// Not checking the variance because it can actually be distorted by the improper
		// convolution
	}

	private NormalDistribution calculateDuration(RelativeMarkovChain chain) {
		for (String state : chain.getRequestStates()) {
			chain.removeState(state, NormalDistribution.ZERO);
		}

		return chain.getTransition(RelativeMarkovChain.INITIAL_STATE, RelativeMarkovChain.FINAL_STATE).getThinkTime();
	}

}
