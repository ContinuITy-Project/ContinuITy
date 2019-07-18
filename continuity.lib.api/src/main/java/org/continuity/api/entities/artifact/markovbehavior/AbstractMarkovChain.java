package org.continuity.api.entities.artifact.markovbehavior;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public abstract class AbstractMarkovChain<T extends MarkovTransition> {

	/**
	 * The initial state of the Markov state.
	 */
	public static final String INITIAL_STATE = "INITIAL*";

	/**
	 * The final (absorbing) state of the Markov chain.
	 */
	public static final String FINAL_STATE = "$";

	private String id;

	private double frequency;

	@JsonDeserialize(as = TreeMap.class, contentAs = TreeMap.class)
	@JsonProperty
	private final Map<String, Map<String, T>> transitions;

	protected AbstractMarkovChain() {
		this(new TreeMap<>());
	}

	protected AbstractMarkovChain(Map<String, Map<String, T>> transitions) {
		this.transitions = transitions;
	}

	protected abstract T createEmptyTransition();

	/**
	 * Gets the ID identifying this Markov chain, e.g., connecting it with a WESSBAS behavior model.
	 * Can be {@code null}.
	 *
	 * @return The ID or {@code null}, if no ID has been set.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the ID of this Markov chain, e.g., for connecting it with a WESSBAS behavior model.
	 *
	 * @param id
	 *            The ID to be set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the frequency of this Markov chain in relation to the other Markov chains contained in
	 * the {@link MarkovBehaviorModel}.
	 *
	 * @return A {@code double} between 0.0 and 1.0.
	 */
	public double getFrequency() {
		return frequency;
	}

	/**
	 * Sets the frequency of this Markov chain in relation to the other Markov chains contained in
	 * the {@link MarkovBehaviorModel}.
	 *
	 * @param frequency
	 *            A {@code double} between 0.0 and 1.0.
	 */
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	/**
	 * Gets the stored transitions. To be handled with care!
	 *
	 * @return The transitions. <b>Do not change!</b>
	 *
	 * @see #getTransition(String, String)
	 * @see #setTransition(String, String, RelativeMarkovTransition)
	 */
	public Map<String, Map<String, T>> getTransitions() {
		return transitions;
	}

	/**
	 * Returns a list of all request states contained in the Markov chain, i.e., all requests that
	 * are not {@value #INITIAL_STATE} or {@value #FINAL_STATE}. <br>
	 * <i>Please note that changing the returned list will not have any effect.</i>
	 *
	 * @return The list of request states.
	 */
	@JsonIgnore
	public List<String> getRequestStates() {
		return transitions.keySet().stream().filter(s -> !INITIAL_STATE.equals(s) && !FINAL_STATE.equals(s)).collect(Collectors.toList());
	}

	/**
	 * Returns the number of request states contained in the Markov chain, i.e., all requests that
	 * are not {@value #INITIAL_STATE} or {@value #FINAL_STATE}.
	 *
	 * @return The number of request states.
	 */
	@JsonIgnore
	public int getNumberOfRequestStates() {
		return transitions.size() - 2;
	}

	/**
	 * Returns the {@link RelativeMarkovTransition} from the state {@code from} to the state {@code to}.
	 * <br>
	 * <i>Changing the returned transition might not have any effect. Please make sure to call
	 * {@link RelativeMarkovChain#setTransition(String, String, RelativeMarkovTransition)}.</i>
	 *
	 * @param from
	 *            The source state of the transition.
	 * @param to
	 *            The destination state of the transition.
	 * @return The transition.
	 */
	public T getTransition(String from, String to) {
		if (!transitions.containsKey(from)) {
			return createEmptyTransition();
		}

		T tran = transitions.get(from).get(to);

		if (tran == null) {
			return createEmptyTransition();
		} else {
			return tran;
		}
	}

	/**
	 * Sets the {@link RelativeMarkovTransition} from the state {@code from} to the state {@code to}. Will
	 * overwrite an existing transition.
	 *
	 * @param from
	 *            The source state of the transition.
	 * @param to
	 *            The destination state of the transition.
	 * @param transition
	 *            The transition.
	 */
	public void setTransition(String from, String to, T transition) {
		if ((transition == null) || (transition.hasZeroProbability())) {
			Map<String, T> fromTransitions = transitions.get(from);

			if (from != null) {
				fromTransitions.remove(to);
			}
		} else {
			Map<String, T> fromTransitions = initState(from);
			initState(to);

			fromTransitions.put(to, transition);
		}
	}

	/**
	 * Adds a new Markov state that has no incoming or outgoing transitions.
	 *
	 * @param state
	 *            The state to be added.
	 */
	public void addState(String state) {
		initState(state);
	}

	protected Map<String, T> initState(String state) {
		Map<String, T> stateTransitions = transitions.get(state);

		if (stateTransitions == null) {
			stateTransitions = new TreeMap<>();
			transitions.put(state, stateTransitions);
		}

		return stateTransitions;
	}

}
