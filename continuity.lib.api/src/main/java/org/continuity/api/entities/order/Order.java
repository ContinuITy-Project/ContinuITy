package org.continuity.api.entities.order;

import java.util.List;
import java.util.Set;

import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.dsl.context.Context;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "target", "mode", "app-id", "services", "version", "testing-context", "context", "options", "source" })
public class Order {

	@JsonProperty("app-id")
	private AppId appId;

	@JsonInclude(Include.NON_EMPTY)
	private List<ServiceSpecification> services;

	@JsonInclude(Include.NON_NULL)
	private VersionOrTimestamp version;

	private ArtifactType target;

	@JsonProperty("testing-context")
	@JsonInclude(Include.NON_EMPTY)
	private Set<String> testingContext;

	@JsonInclude(Include.NON_NULL)
	private Context context;

	@JsonInclude(Include.NON_NULL)
	private ArtifactExchangeModel source;

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

	public ArtifactType getTarget() {
		return target;
	}

	public void setTarget(ArtifactType target) {
		this.target = target;
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

	public ArtifactExchangeModel getSource() {
		return source;
	}

	public void setSource(ArtifactExchangeModel source) {
		this.source = source;
	}

	public OrderOptions getOptions() {
		return options;
	}

	public void setOptions(OrderOptions options) {
		this.options = options;
	}

}
