package org.continuity.api.entities.deserialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;

/**
 * Provides method for (de-) serialization of multidimensional arrays (csv) and {@link Behavior}
 * objects.
 * 
 * @author Tobias Angerstein
 *
 */
public class BehaviorModelSerializer {

	/**
	 * Default zero edge.
	 */
	private static final String DEFAULT_ZERO_EDGE = "0.0; n(0 0)";

	/**
	 * Deserializes the behaviorModel csv representation into a {@link Behavior}
	 * 
	 * @param csvRepresentation
	 *            the csv representation of a {@link Behavior}
	 * @return {@link Behavior}
	 */
	public static Behavior deserializeBehaviorModel(String[][] csvRepresentation) {
		if (null == csvRepresentation) {
			return null;
		}
		List<MarkovState> markovStates = new ArrayList<MarkovState>();
		for (int i = 1; i < csvRepresentation.length; i++) {
			MarkovState markovState = new MarkovState();
			markovState.setId(csvRepresentation[i][0]);

			List<Transition> transitions = new ArrayList<Transition>();

			for (int j = 1; j < csvRepresentation[i].length; j++) {
				if (!csvRepresentation[i][j].equals(DEFAULT_ZERO_EDGE)) {
					Transition transition = new Transition();

					Pattern pattern = Pattern.compile("[0-9.]+");
					Matcher matcher = pattern.matcher(csvRepresentation[i][j]);
					matcher.find();
					transition.setProbability(Double.parseDouble(matcher.group(0)));

					matcher.find();
					transition.setMean(Double.parseDouble(matcher.group(0)));

					matcher.find();
					transition.setDeviation(Double.parseDouble(matcher.group(0)));

					transition.setTargetState(csvRepresentation[0][j]);

					transitions.add(transition);
				}
			}
			markovState.setTransitions(transitions);
			markovStates.add(markovState);
		}

		// Add ExitState
		MarkovState exitState = new MarkovState();
		exitState.setId("$");
		exitState.setTransitions(Collections.EMPTY_LIST);
		markovStates.add(exitState);

		Behavior behavior = new Behavior();
		behavior.setInitialState(markovStates.get(0).getId());
		behavior.setMarkovStates(markovStates);
		return behavior;
	}

	/**
	 * Provides serialization of {@link Behavior} into a multidimensional array representation.
	 * 
	 * @param rootBehaviorModel
	 *            the behavior model, which has to be serialized.
	 * @return {@link String[][]}
	 */
	public static String[][] serializeBehaviorModel(Behavior rootBehaviorModel) {
		String[][] csvRepresentation = new String[rootBehaviorModel.getMarkovStates().size()][rootBehaviorModel.getMarkovStates().size() + 1];
		List<String> markovStateNames = rootBehaviorModel.getMarkovStates().stream().map(ms -> ms.getId()).collect(Collectors.toList());

		// Remove ExitState
		MarkovState exitState = new MarkovState();
		for(MarkovState state : rootBehaviorModel.getMarkovStates()) {
			if(state.getId().equals("$")) {
				exitState = state;
				break;
			}
		}
		rootBehaviorModel.getMarkovStates().remove(exitState);

		for (int i = 0; i < rootBehaviorModel.getMarkovStates().size(); i++) {

			// Write MarkovState name
			csvRepresentation[i + 1][0] = rootBehaviorModel.getMarkovStates().get(i).getId();

			// Write transitions
			for (Transition transition : rootBehaviorModel.getMarkovStates().get(i).getTransitions()) {
				csvRepresentation[i + 1][markovStateNames.indexOf(transition.getTargetState()) + 1] = getTransitionString(transition);
			}
		}
		// Write target state names (y-axis)
		for (int j = 0; j < markovStateNames.size(); j++) {
			if(markovStateNames.get(j).equals("INITIAL*")) {
				csvRepresentation[0][j + 1] = "INITIAL";
				continue;
			}
			csvRepresentation[0][j + 1] = markovStateNames.get(j);
		}

		fillEmptyFields(csvRepresentation);

		return csvRepresentation;
	}

	/**
	 * Fills empty fields with predefined zero edge, except csvRepresentation[0][0]
	 * 
	 * @param csvRepresentation
	 *            {@link String[][]}
	 */
	private static void fillEmptyFields(String[][] csvRepresentation) {
		for (int i = 0; i < csvRepresentation.length; i++) {
			for (int j = 0; j < csvRepresentation[i].length; j++) {
				if (i == 0 && j == 0) {
					csvRepresentation[i][j] = "";
				} else if (csvRepresentation[i][j] == null) {
					csvRepresentation[i][j] = DEFAULT_ZERO_EDGE;
				}
			}
		}
	}

	/**
	 * Provides transition properties String
	 * 
	 * @param transition
	 * @return
	 */
	private static String getTransitionString(Transition transition) {
		return String.format("%f; n(%f %f)", transition.getProbability(), transition.getMean(), transition.getDeviation());
	}

}
