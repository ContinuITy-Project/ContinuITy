package org.continuity.orchestrator.orders;

import java.util.List;

import org.continuity.api.entities.order.OrderGoal;
import org.continuity.api.entities.order.OrderMode;

public class OrderCycle {

	private final OrderMode mode;

	private final List<OrderGoal> goals;

	public OrderCycle(OrderMode mode, List<OrderGoal> goals) {
		this.mode = mode;
		this.goals = goals;
	}

	public OrderMode getMode() {
		return mode;
	}

	public List<OrderGoal> getGoals() {
		return goals;
	}

}
