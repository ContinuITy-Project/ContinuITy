package org.continuity.cobra.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.assertj.core.data.Offset;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovTransition;
import org.continuity.api.entities.test.MarkovChainTestInstance;
import org.junit.Test;

public class ClustinatorMarkovChainTest {

	private Random rand = new Random(42);

	@Test
	public void testMarkovArrayMarkov() throws IOException {
		testMarkovArrayMarkov(MarkovChainTestInstance.SIMPLE);
		testMarkovArrayMarkov(MarkovChainTestInstance.SIMPLE_INSERT);
		testMarkovArrayMarkov(MarkovChainTestInstance.SIMPLE_WITH_INSERT);
		testMarkovArrayMarkov(MarkovChainTestInstance.SIMPLE_WO_A);
		testMarkovArrayMarkov(MarkovChainTestInstance.SOCK_SHOP);
		testMarkovArrayMarkov(MarkovChainTestInstance.SOCK_SHOP_WO_CART);
	}

	private void testMarkovArrayMarkov(MarkovChainTestInstance instance) throws IOException {
		RelativeMarkovChain orig = RelativeMarkovChain.fromCsv(instance.getCsv());

		// Using the probabilities as counts, as the actual counts are missing
		orig.getTransitions().values().stream().map(Map::values).flatMap(Collection::stream).forEach(t -> t.setCount(t.getProbability() * 5));

		List<String> states = orig.getRequestStates();
		states.add(0, RelativeMarkovChain.INITIAL_STATE);
		states.add(RelativeMarkovChain.FINAL_STATE);
		ClustinatorMarkovChainConverter converter = new ClustinatorMarkovChainConverter(states);

		double[] array = converter.convertMarkovChain(orig);
		RelativeMarkovChain converted = converter.convertArray(array, zeroArray(array.length), zeroArray(array.length), zeroArray(array.length));

		for (String state : states) {
			assertThat(converted.getTransitions().get(state).values().stream().mapToDouble(RelativeMarkovTransition::getProbability).toArray()).as("transition probabilities from state " + state)
					.containsExactly(orig.getTransitions().get(state).values().stream().mapToDouble(RelativeMarkovTransition::getProbability).toArray(), Offset.offset(0.0001));
		}
	}

	@Test
	public void testArrayMarkovArray() {
		testArrayMarkovArray(5);
		testArrayMarkovArray(1);
		testArrayMarkovArray(42);
		testArrayMarkovArray(0);
		testArrayMarkovArray(5);
		testArrayMarkovArray(10);
		testArrayMarkovArray(13);
		testArrayMarkovArray(25);
	}

	private void testArrayMarkovArray(int n) {
		List<String> states = Stream.iterate(1, i -> i + 1).limit(n).map(i -> "state" + i).collect(Collectors.toList());
		states.add(0, RelativeMarkovChain.INITIAL_STATE);
		states.add(RelativeMarkovChain.FINAL_STATE);
		double[] array = arrayForStates(states);
		testArrayMarkovArray(states, array);
	}

	private double[] arrayForStates(List<String> states) {
		int n = states.size();

		List<Double> list = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			double[] sub = rand.doubles(n, 0, 1).toArray();
			double sum = Arrays.stream(sub).sum();
			Arrays.stream(sub).map(d -> d / sum).forEach(list::add);
		}

		return list.stream().mapToDouble(d -> d).toArray();
	}

	private void testArrayMarkovArray(List<String> states, double[] array) {
		ClustinatorMarkovChainConverter converter = new ClustinatorMarkovChainConverter(states);

		RelativeMarkovChain chain = converter.convertArray(array, zeroArray(array.length), zeroArray(array.length), zeroArray(array.length));
		double[] converted = converter.convertMarkovChain(chain);

		assertThat(converted).isEqualTo(array);
	}

	private double[] zeroArray(int length) {
		return DoubleStream.generate(() -> 0D).limit(length).toArray();
	}

}
