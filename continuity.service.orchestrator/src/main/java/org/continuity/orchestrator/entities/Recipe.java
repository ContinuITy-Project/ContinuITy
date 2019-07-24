package org.continuity.orchestrator.entities;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.dsl.description.ForecastInput;
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

	private LinkExchangeModel source;

	private final OrderOptions options;

	private ForecastInput forecastInput;

	private final boolean longTermUse;

	private final Set<String> testingContext;

	public Recipe(String orderId, String recipeId, AppId aid, List<ServiceSpecification> services, VersionOrTimestamp version, List<RecipeStep> steps, LinkExchangeModel source, boolean longTermUse,
			Set<String> testingContext,
			OrderOptions options, ForecastInput forecastInput) {
		this.orderId = orderId;
		this.recipeId = recipeId;
		this.iterator = steps.listIterator(steps.size());
		this.appId = aid;
		this.services = services;
		this.version = version;
		this.source = source;
		this.longTermUse = longTermUse;
		this.testingContext = testingContext;
		this.setForecastInput(forecastInput);
		this.options = options;
		initIterator(source);
	}

	public String getOrderId() {
		return orderId;
	}

	public String getRecipeId() {
		return recipeId;
	}

	public LinkExchangeModel getSource() {
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
		task.setSource(source);
		task.setOptions(options);
		task.setForecastInput(forecastInput);
		task.setLongTermUse(longTermUse);

		nextStep.setTask(task);

		return nextStep;
	}

	public void updateFromReport(TaskReport report) {
		source.merge(report.getResult());
	}

	private void initIterator(LinkExchangeModel source) {
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

	public ForecastInput getForecastInput() {
		return forecastInput;
	}

	public void setForecastInput(ForecastInput forecastInput) {
		this.forecastInput = forecastInput;
	}

}
