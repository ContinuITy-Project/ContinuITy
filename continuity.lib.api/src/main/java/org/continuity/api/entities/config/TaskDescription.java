package org.continuity.api.entities.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.dsl.context.Context;
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

	private LinkExchangeModel source;

	@JsonProperty("long-term-use")
	private boolean longTermUse;

	private OrderOptions options;

	private Context context;

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

	public LinkExchangeModel getSource() {
		return source;
	}

	public void setSource(LinkExchangeModel source) {
		this.source = source;
	}

	public boolean isLongTermUse() {
		return longTermUse;
	}

	public void setLongTermUse(boolean longTermUse) {
		this.longTermUse = longTermUse;
	}

	public OrderOptions getOptions() {
		return options;
	}

	public void setOptions(OrderOptions options) {
		this.options = options;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
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
