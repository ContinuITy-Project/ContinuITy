package org.continuity.api.entities.artifact.markovbehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Markov chain that is part of a {@link MarkovBehaviorModel}. It is especially
 * designed for operations such as replacing states by other Markov chains or removing states. Each
 * Markov state represents a request to a certain endpoint that is represented by an ID. Besides the
 * request states, it holds an initial state {@value #INITIAL_STATE} and a final state
 * {@value #FINAL_STATE}. <br>
 *
 * A MarkovChain can be read from a CSV file conforming to the WESSBAS behavior model and also
 * written into the same format. <i>Please note that the WESSBAS format deals with think time
 * standard deviations, while this representation holds the variances. However, the values are
 * automatically transformed by {@link #fromCsv(String[][])} and {@link #toCsv()}.</i>
 *
 * @author Henning Schulz
 *
 */
public class RelativeMarkovChain extends AbstractMarkovChain<RelativeMarkovTransition> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RelativeMarkovChain.class);

	private static final Pattern TRANSITION_PATTERN = Pattern.compile("(.+); n\\((.+) (.+)\\)");

	private static final String SUB_INITIAL_STATE = "SUB_INITIAL*";

	private static final String SUB_FINAL_STATE = "SUB_$";

	private static final String INITIAL_STATE_COL_HEADER = "INITIAL";

	private static final String DEFAULT_ZERO_TRANSITION = "0.0; n(0 0)";

	public RelativeMarkovChain() {
	}

	protected RelativeMarkovChain(Map<String, Map<String, RelativeMarkovTransition>> transitions) {
		super(transitions);
	}

	/**
	 * Parses a {@link RelativeMarkovChain} from a 2-dimensional matrix, as represented by a CSV file.
	 * Requires the matrix to be formatted as follows:
	 *
	 * <table>
	 * <tr>
	 * <td></td>
	 * <td>INITIAL*</td>
	 * <td>requestId</td>
	 * <td>$</td>
	 * </tr>
	 * <tr>
	 * <td>INITIAL</td>
	 * <td>x; n(x x)</td>
	 * <td>x; n(x x)</td>
	 * <td>x; n(x x)</td>
	 * </tr>
	 * <tr>
	 * <td>requestId</td>
	 * <td>x; n(x x)</td>
	 * <td>x; n(x x)</td>
	 * <td>x; n(x x)</td>
	 * </tr>
	 * </table>
	 *
	 * @param csv
	 *            The CSV file's content as matrix.
	 * @return The parsed {@link RelativeMarkovChain}.
	 */
	public static RelativeMarkovChain fromCsv(String[][] csv) {
		RelativeMarkovChain chain = new RelativeMarkovChain();

		for (int row = 1; row < csv.length; row++) {
			String rowName = csv[row][0];

			for (int col = 1; col < csv[row].length; col++) {
				String colName = csv[0][col];

				if (INITIAL_STATE_COL_HEADER.equals(colName)) {
					colName = INITIAL_STATE;
				}

				String transition = csv[row][col];
				Matcher matcher = TRANSITION_PATTERN.matcher(transition);

				if (matcher.matches()) {
					double prob = Double.parseDouble(matcher.group(1));
					double ttMean = Double.parseDouble(matcher.group(2));
					double ttDev = Double.parseDouble(matcher.group(3));

					if (prob > 0) {
						chain.setTransition(rowName, colName, new RelativeMarkovTransition(prob, ttMean, Math.pow(ttDev, 2)));
					}
				} else {
					throw new IllegalArgumentException("Illegally formatted transition: " + transition);
				}
			}
		}

		chain.initState(FINAL_STATE);
		chain.setTransition(FINAL_STATE, FINAL_STATE, new RelativeMarkovTransition(1, 0, 0));

		return chain;
	}

	/**
	 * Writes the represented Markov chain into a 2-dimensional matrix.
	 *
	 * @return The generated matrix.
	 * @see RelativeMarkovChain#fromCsv(String[][])
	 */
	public String[][] toCsv() {
		List<String> allStates = getRequestStates();
		allStates.add(0, INITIAL_STATE);
		allStates.add(FINAL_STATE);

		int numStates = allStates.size() + 1;

		List<String[]> csv = new ArrayList<>();

		for (String state : allStates) {
			if (!FINAL_STATE.equals(state)) {
				csv.add(toCsvRow(state, allStates));
			}
		}

		allStates.add(0, "");
		String[] headers = allStates.toArray(new String[numStates]);
		headers[1] = INITIAL_STATE_COL_HEADER;
		csv.add(0, headers);

		return csv.toArray(new String[numStates - 1][]);
	}

	private String[] toCsvRow(String state, List<String> allStates) {
		Map<String, RelativeMarkovTransition> outgoingTransitions = getTransitions().get(state);
		List<String> transitionStrings = allStates.stream().map(s -> outgoingTransitions.get(s) == null ? DEFAULT_ZERO_TRANSITION : outgoingTransitions.get(s).toString()).collect(Collectors.toList());
		transitionStrings.add(0, state);
		return transitionStrings.toArray(new String[allStates.size() + 1]);
	}

	/**
	 * Removes states from the Markov chain and fixes the broken getTransitions() to and from the removed
	 * states. The states are identified by applying a predicate to all existing states.
	 *
	 * @param stateIdentifier
	 *            The predicate identifying the states to be removed. That is, if the predicate
	 *            returns {@code true}, the state will be removed.
	 * @param stateDuration
	 *            The duration of the state to be removed. This duration will be added to the think
	 *            times of the getTransitions() from the state's predecessors to the successors.
	 * @return The number of states that have been removed.
	 */
	public int removeStates(Predicate<String> stateIdentifier, NormalDistribution stateDuration) {
		List<String> statesToRemove = getTransitions().keySet().stream().filter(stateIdentifier).collect(Collectors.toList());

		for (String state : statesToRemove) {
			removeState(state, stateDuration);
		}

		return statesToRemove.size();
	}

	/**
	 * Removes a state from the Markov chain and fixes the broken getTransitions() to and from the
	 * removed state.
	 *
	 * @param state
	 *            The state to be removed. Cannot be {@value #INITIAL_STATE} or
	 *            {@value #FINAL_STATE}.
	 * @param stateDuration
	 *            The duration of the state to be removed. This duration will be added to the think
	 *            times of the getTransitions() from the state's predecessors to the successors.
	 */
	public void removeState(String state, NormalDistribution stateDuration) {
		if (getTransitions().get(state) == null) {
			throw new IllegalArgumentException("State " + state + " is not contained in the Markov chain!");
		}

		LOGGER.debug("Markov chain {}: Removing state {}...", getId(), state);

		removeCycle(state, stateDuration);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Markov chain {}: Incoming getTransitions() to {}: {}", getId(), state, incomingTransitions(state).collect(Collectors.toList()));
		}

		incomingTransitions(state)
				.flatMap(pair -> getTransitions().get(state).entrySet().stream().map(e -> Triple.of(pair.getKey(), e.getKey(), RelativeMarkovTransition.concatenate(pair.getValue(), stateDuration, e.getValue()))))
				.forEach(triple -> {
					RelativeMarkovTransition orig = getTransitions().get(triple.getLeft()).get(triple.getMiddle());
					RelativeMarkovTransition concat = triple.getRight();

					if (orig != null) {
						LOGGER.debug("Markov chain {}: Merging getTransitions() {}->{}->{} ({}) and {}->{} ({}).", getId(), triple.getLeft(), state, triple.getMiddle(), concat, triple.getLeft(),
								triple.getMiddle(), orig);
						concat = RelativeMarkovTransition.merge(orig, concat);
					}

					setTransition(triple.getLeft(), triple.getMiddle(), concat);

					LOGGER.debug("Markov chain {}: Concatenated transition {}->{}->{} to {}.", getId(), triple.getLeft(), state, triple.getMiddle(), concat);
				});

		getTransitions().forEach((from, trans) -> trans.remove(state));
		getTransitions().remove(state);
	}

	/**
	 * Replaces a Markov state with another Markov chain that will be integrated into this chain.
	 * The initial and final states of the inserted chain will be removed. All added states will be
	 * renamed to {@code state#subStateName}, where {@code state} is the replaced state and
	 * {@code subStateName} is the name of the inserted state. <br>
	 * <i>Please not that the subChain will be destroyed.</i>
	 *
	 * @param state
	 *            The state to be replaced.
	 * @param subChain
	 *            The Markov chain to be added as a replacement of {@code state}.
	 */
	public void replaceState(String state, RelativeMarkovChain subChain) {
		LOGGER.debug("Markov chain {}: Replacing state {} with {} new states...", getId(), state, subChain.getNumberOfRequestStates());

		for (String subState : subChain.getRequestStates()) {
			String newStateName = state + "#" + subState;
			subChain.renameState(subState, newStateName);
			LOGGER.debug("Markov chain {}: Will add state {}.", getId(), newStateName);
		}

		subChain.renameState(INITIAL_STATE, SUB_INITIAL_STATE);
		subChain.renameState(FINAL_STATE, SUB_FINAL_STATE);
		subChain.setTransition(SUB_FINAL_STATE, SUB_FINAL_STATE, null);

		getTransitions().putAll(subChain.getTransitions());

		incomingTransitions(state).forEach(pair -> {
			getTransitions().get(pair.getKey()).put(SUB_INITIAL_STATE, pair.getValue());
			getTransitions().get(pair.getKey()).remove(state);
		});

		getTransitions().get(state).forEach((to, tran) -> {
			getTransitions().get(SUB_FINAL_STATE).put(to, tran);
		});

		getTransitions().remove(state);

		removeState(SUB_INITIAL_STATE, NormalDistribution.ZERO);
		removeState(SUB_FINAL_STATE, NormalDistribution.ZERO);
	}

	public void renameState(String origName, String newName) {
		LOGGER.debug("Markov chain {}: Renaming state {} to {}.", getId(), origName, newName);

		for (Pair<String, RelativeMarkovTransition> pair : incomingTransitions(origName).collect(Collectors.toList())) {
			setTransition(pair.getKey(), newName, pair.getValue());
			getTransitions().get(pair.getLeft()).remove(origName);
		}

		getTransitions().put(newName, getTransitions().get(origName));
		getTransitions().remove(origName);
	}

	private Stream<Pair<String, RelativeMarkovTransition>> incomingTransitions(String state) {
		return getTransitions().entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue().get(state))).filter(p -> p.getValue() != null);
	}

	private void removeCycle(String state, NormalDistribution stateDuration) {
		RelativeMarkovTransition cycle = getTransition(state, state);

		if (cycle.getProbability() == 0.0) {
			return;
		}

		double p = cycle.getProbability();
		double expectedSteps = (1.0 / (1.0 - p)) - 1.0; // geometric series
		double overallProb = getTransitions().get(state).entrySet().stream().filter(e -> !state.equals(e.getKey())).map(e -> e.getValue().getProbability()).reduce(Double::sum).orElseGet(() -> 1.0);
		NormalDistribution additionalThinkTime = NormalDistribution.add(cycle.getThinkTime(), stateDuration);

		LOGGER.debug("Markov chain {}: Removing cycle of {} with {} expected steps.", getId(), state, expectedSteps);

		for (RelativeMarkovTransition outgoing : getTransitions().get(state).values()) {
			if (outgoing != cycle) { // It's really about object identity
				outgoing.setProbability(outgoing.getProbability() / overallProb);
				outgoing.setThinkTime(NormalDistribution.combine(1, outgoing.getThinkTime(), expectedSteps, additionalThinkTime));
			}
		}

		getTransitions().get(state).remove(state);
	}

	@Override
	protected RelativeMarkovTransition createEmptyTransition() {
		return new RelativeMarkovTransition();
	}

}
