package org.continuity.api.entities.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.dsl.WorkloadDescription;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TaskDescription {

	/**
	 * recipeId.subtaskId
	 */
	private String taskId;

	@JsonProperty("app-id")
	private AppId appId;

	@JsonInclude(Include.NON_EMPTY)
	private List<ServiceSpecification> services;

	private VersionOrTimestamp version;

	@JsonInclude(Include.NON_NULL)
	private LocalDateTime perspective;

	private ArtifactExchangeModel source;

	@JsonProperty("long-term-use")
	private boolean longTermUse;

	private ArtifactType target;

	private OrderOptions options;

	@JsonProperty("workload-description")
	private WorkloadDescription workloadDescription;

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

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

	public LocalDateTime getPerspective() {
		return perspective;
	}

	public void setPerspective(LocalDateTime perspective) {
		this.perspective = perspective;
	}

	public ArtifactExchangeModel getSource() {
		return source;
	}

	public void setSource(ArtifactExchangeModel source) {
		this.source = source;
	}

	public boolean isLongTermUse() {
		return longTermUse;
	}

	public void setLongTermUse(boolean longTermUse) {
		this.longTermUse = longTermUse;
	}

	public ArtifactType getTarget() {
		return target;
	}

	public void setTarget(ArtifactType target) {
		this.target = target;
	}

	public OrderOptions getOptions() {
		return options;
	}

	@JsonIgnore
	public OrderOptions getOptionsOrDefault() {
		return options == null ? new OrderOptions() : options;
	}

	public void setOptions(OrderOptions options) {
		this.options = options;
	}

	public WorkloadDescription getWorkloadDescription() {
		return workloadDescription;
	}

	public void setWorkloadDescription(WorkloadDescription workloadDescription) {
		this.workloadDescription = workloadDescription;
	}

	/**
	 * Gets the services that are effectively to be considered for tailoring. Takes into account the
	 * app-id and the separately specified services.
	 *
	 * @return
	 */
	@JsonIgnore
	public List<ServiceSpecification> getEffectiveServices() {
		List<ServiceSpecification> effectiveServices = new ArrayList<>();

		if (services != null) {
			effectiveServices.addAll(services);
		} else if (appId != null) {
			effectiveServices.add(new ServiceSpecification(appId.getService(), version));
		}

		if (effectiveServices.isEmpty()) {
			effectiveServices.add(new ServiceSpecification(AppId.SERVICE_ALL, version));
		}

		return effectiveServices.stream().map(s -> s.withVersionFallback(version)).collect(Collectors.toList());
	}

}
