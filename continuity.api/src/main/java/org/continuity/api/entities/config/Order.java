package org.continuity.api.entities.config;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Order {

	private String tag;

	private OrderGoal goal;

	private LinkExchangeModel source;

	@JsonInclude(Include.NON_NULL)
	private OrderOptions options;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public OrderGoal getGoal() {
		return goal;
	}

	public void setGoal(OrderGoal goal) {
		this.goal = goal;
	}

	public LinkExchangeModel getSource() {
		return source;
	}

	public void setSource(LinkExchangeModel source) {
		this.source = source;
	}

	public OrderOptions getOptions() {
		return options;
	}

	public void setOptions(OrderOptions options) {
		this.options = options;
	}

}
