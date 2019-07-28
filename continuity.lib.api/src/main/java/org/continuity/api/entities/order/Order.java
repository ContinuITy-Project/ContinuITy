package org.continuity.api.entities.order;

import java.util.List;
import java.util.Set;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.dsl.context.Context;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "goal", "mode", "app-id", "services", "version", "testing-context", "context", "options", "source" })
public class Order {

	@JsonProperty("app-id")
	private AppId appId;

	@JsonInclude(Include.NON_EMPTY)
	private List<ServiceSpecification> services;

	@JsonInclude(Include.NON_NULL)
	private VersionOrTimestamp version;

	private OrderGoal goal;

	@JsonInclude(Include.NON_NULL)
	private OrderMode mode;

	@JsonProperty("testing-context")
	@JsonInclude(Include.NON_EMPTY)
	private Set<String> testingContext;

	@JsonInclude(Include.NON_NULL)
	private Context context;

	@JsonInclude(Include.NON_NULL)
	private LinkExchangeModel source;

	@JsonInclude(Include.NON_NULL)
	private OrderOptions options;

	public AppId getAppId() {
		return appId;
	}

	public void setAppId(AppId appId) {
		this.appId = appId;
	}

	public List<ServiceSpecification> getServices() {
		return services;
	}

	public void setServices(List<ServiceSpecification> services) {
		this.services = services;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
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

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
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
