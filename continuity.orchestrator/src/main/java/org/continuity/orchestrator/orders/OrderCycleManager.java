package org.continuity.orchestrator.orders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.continuity.api.entities.config.OrderGoal;
import org.continuity.api.entities.config.OrderMode;

/**
 * Manages possible cycles through the pipeline.
 *
 * @author Henning Schulz
 *
 */
public class OrderCycleManager {

	private final Map<OrderMode, Set<OrderCycle>> cycles = new HashMap<>();

	/**
	 * Initializes the manager by defining the possible cycles.
	 */
	public OrderCycleManager() {
		cycle(OrderMode.PAST_SESSIONS, OrderGoal.CREATE_SESSION_LOGS, OrderGoal.CREATE_WORKLOAD_MODEL, OrderGoal.CREATE_LOAD_TEST, OrderGoal.EXECUTE_LOAD_TEST);
		cycle(OrderMode.PAST_REQUESTS, OrderGoal.CREATE_WORKLOAD_MODEL, OrderGoal.CREATE_LOAD_TEST, OrderGoal.EXECUTE_LOAD_TEST);
		cycle(OrderMode.FORECASTED_WORKLOAD, OrderGoal.CREATE_SESSION_LOGS, OrderGoal.CREATE_BEHAVIOR_MIX, OrderGoal.CREATE_FORECAST, OrderGoal.CREATE_WORKLOAD_MODEL, OrderGoal.CREATE_LOAD_TEST, OrderGoal.EXECUTE_LOAD_TEST);
	}

	/**
	 * Defines a new cycle through the pipeline. It has to be ensured that one goal can be contained
	 * in at most one cycle of a mode.
	 *
	 * @param mode
	 * @param goals
	 */
	private void cycle(OrderMode mode, OrderGoal... goals) {
		Set<OrderCycle> cyclesForMode = cycles.get(mode);

		if (cyclesForMode == null) {
			cyclesForMode = new HashSet<>();
			cycles.put(mode, cyclesForMode);
		}

		cyclesForMode.add(new OrderCycle(mode, Arrays.asList(goals)));
	}

	/**
	 * Gets the cycle to reach a goal in a mode. That is, if the goal is C and there is a cycle
	 * A->B->C->D for the mode, A->B->C will be returned.
	 *
	 * @param mode
	 * @param goal
	 * @return
	 */
	public List<OrderGoal> getCycle(OrderMode mode, OrderGoal goal) {
		for (OrderCycle cycle : cycles.get(mode)) {
			int idx = cycle.getGoals().indexOf(goal);

			if (idx >= 0) {
				return cycle.getGoals().subList(0, idx + 1);
			}
		}

		return null;
	}

	/**
	 * Gets all cycles for a mode.
	 *
	 * @param mode
	 * @return
	 */
	public Set<OrderCycle> getFullCycles(OrderMode mode) {
		return cycles.get(mode);
	}

	/**
	 * Gets all modes containing a cycle containing the specified goal.
	 *
	 * @param goal
	 * @return
	 */
	public List<OrderMode> getModesContainingGoal(OrderGoal goal) {
		List<OrderMode> modes = new ArrayList<>();

		for (Entry<OrderMode, Set<OrderCycle>> entry : cycles.entrySet()) {
			Optional<Boolean> contained = entry.getValue().stream().map(cycle -> cycle.getGoals().contains(goal)).reduce(Boolean::logicalOr);

			if (contained.isPresent() && contained.get().booleanValue()) {
				modes.add(entry.getKey());
			}
		}

		return modes;
	}

}
