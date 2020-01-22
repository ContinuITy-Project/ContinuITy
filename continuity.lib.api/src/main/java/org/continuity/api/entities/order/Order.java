package org.continuity.api.entities.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.dsl.WorkloadDescription;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "target", "app-id", "services", "version", "perspective", "forecast-total", "testing-context", "workload-description", "options", "source" })
public class Order {

	@JsonProperty("app-id")
	private AppId appId;

	@JsonInclude(Include.NON_EMPTY)
	private List<ServiceSpecification> services;

	@JsonInclude(Include.NON_NULL)
	private VersionOrTimestamp version;

	private ArtifactType target;

	@JsonInclude(Include.NON_NULL)
	private LocalDateTime perspective;

	@JsonProperty("forecast-total")
	@JsonInclude(Include.NON_ABSENT)
	private Optional<Boolean> forecastTotal = Optional.empty();

	@JsonProperty("testing-context")
	@JsonInclude(Include.NON_EMPTY)
	private Set<String> testingContext;

	@JsonProperty("workload-description")
	@JsonInclude(Include.NON_NULL)
	private WorkloadDescription workloadDescription;

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

	public LocalDateTime getPerspective() {
		return perspective;
	}

	public void setPerspective(LocalDateTime perspective) {
		this.perspective = perspective;
	}

	public Optional<Boolean> getForecastTotal() {
		return forecastTotal;
	}

	public void setForecastTotal(Optional<Boolean> forecastTotal) {
		this.forecastTotal = forecastTotal;
	}

	public Set<String> getTestingContext() {
		return testingContext;
	}

	public void setTestingContext(Set<String> testingContext) {
		this.testingContext = testingContext;
	}

	public WorkloadDescription getWorkloadDescription() {
		return workloadDescription;
	}

	public void setWorkloadDescription(WorkloadDescription workloadDescription) {
		this.workloadDescription = workloadDescription;
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
