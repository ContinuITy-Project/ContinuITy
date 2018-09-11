package org.continuity.orchestrator.orders;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.continuity.api.entities.config.OrderGoal;
import org.continuity.api.entities.config.OrderMode;
import org.junit.Test;

public class CyclesValidationTest {

	private final OrderCycleManager manager = new OrderCycleManager();

	@Test
	public void checkForValidCycles() {
		for (OrderMode mode : OrderMode.values()) {
			Set<OrderGoal> goals = new HashSet<>();

			for (OrderCycle cycle : manager.getFullCycles(mode)) {
				for (OrderGoal goal : cycle.getGoals()) {
					if (goals.contains(goal)) {
						fail("The order goal " + goal + " is contained twice in the possible cycles.");
					}

					goals.add(goal);
				}
			}
		}
	}

}
