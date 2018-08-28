package org.continuity.api.entities.config;

import java.util.Set;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "goal", "mode", "tag", "testing-context", "options", "source" })
public class Order {

	private String tag;

	private OrderGoal goal;

	@JsonInclude(Include.NON_NULL)
	private OrderMode mode;

	@JsonProperty("testing-context")
	@JsonInclude(Include.NON_EMPTY)
	private Set<String> testingContext;

	@JsonInclude(Include.NON_NULL)
	private LinkExchangeModel source;

	@JsonInclude(Include.NON_NULL)
	private OrderOptions options;

	@JsonProperty("modularization")
	@JsonInclude(Include.NON_NULL)
	private ModularizationOptions modularizationOptions;

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

	public OrderMode getMode() {
		return mode;
	}

	public void setMode(OrderMode mode) {
		this.mode = mode;
	}

	public Set<String> getTestingContext() {
		return testingContext;
	}

	public void setTestingContext(Set<String> testingContext) {
		this.testingContext = testingContext;
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

	public ModularizationOptions getModularizationOptions() {
		return modularizationOptions;
	}

	public void setModularizationOptions(ModularizationOptions modularizationOptions) {
		this.modularizationOptions = modularizationOptions;
	}

}
