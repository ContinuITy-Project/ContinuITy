package org.continuity.orchestrator.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;

import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.dsl.WorkloadDescription;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

public class Recipe {

	private final String orderId;

	private final String recipeId;

	private final ListIterator<RecipeStep> iterator;

	private int stepCounter = 1;

	private final AppId appId;

	private final List<ServiceSpecification> services;

	private final VersionOrTimestamp version;

	private LocalDateTime perspective;

	private Optional<Boolean> forecastTotal = Optional.empty();

	private ArtifactExchangeModel source;

	private final OrderOptions options;

	private WorkloadDescription workloadDescription;

	private final boolean longTermUse;

	private final Set<String> testingContext;

	public Recipe(String orderId, String recipeId, AppId aid, List<ServiceSpecification> services, VersionOrTimestamp version, LocalDateTime perspective, Optional<Boolean> forecastTotal,
			List<RecipeStep> steps, ArtifactExchangeModel source, boolean longTermUse, Set<String> testingContext, OrderOptions options, WorkloadDescription workloadDescription) {
		this.orderId = orderId;
		this.recipeId = recipeId;
		this.iterator = steps.listIterator(steps.size());
		this.appId = aid;
		this.services = services;
		this.version = version;
		this.perspective = perspective;
		this.forecastTotal = forecastTotal;
		this.source = source == null ? new ArtifactExchangeModel() : source;
		this.longTermUse = longTermUse;
		this.testingContext = testingContext;
		this.workloadDescription = workloadDescription;
		this.options = options;
		initIterator(source);
	}

	public String getOrderId() {
		return orderId;
	}

	public String getRecipeId() {
		return recipeId;
	}

	public ArtifactExchangeModel getSource() {
		return source;
	}

	public AppId getAppId() {
		return appId;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public Set<String> getTestingContext() {
		return testingContext;
	}

	public RecipeStep next() {
		RecipeStep nextStep = iterator.next();

		String taskId = recipeId + "." + stepCounter++ + "-" + nextStep.getName();
		TaskDescription task = new TaskDescription();
		task.setTaskId(taskId);
		task.setAppId(appId);
		task.setServices(services);
		task.setVersion(version);
		task.setPerspective(perspective);
		task.setForecastTotal(forecastTotal);
		task.setSource(source);
		task.setOptions(options);
		task.setWorkloadDescription(workloadDescription);
		task.setLongTermUse(longTermUse);

		nextStep.setTask(task);

		return nextStep;
	}

	public void updateFromReport(TaskReport report) {
		source.merge(report.getResult());
	}

	private void initIterator(ArtifactExchangeModel source) {
		while (iterator.hasPrevious()) {
			boolean stop = iterator.previous().checkData(source);

			if (stop) {
				if (iterator.hasNext()) {
					iterator.next();
				}

				return;
			}
		}
	}

	public WorkloadDescription getWorkloadDescription() {
		return workloadDescription;
	}

	public void setWorkloadDescription(WorkloadDescription workloadDescription) {
		this.workloadDescription = workloadDescription;
	}

}
