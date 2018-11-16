package org.continuity.orchestrator.entities;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.entities.config.OrderOptions;
import org.continuity.api.entities.config.PropertySpecification;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.dsl.description.ForecastInput;

public class Recipe {

	private final String orderId;

	private final String recipeId;

	private final ListIterator<RecipeStep> iterator;

	private int stepCounter = 1;

	private final String tag;

	private LinkExchangeModel source;

	private PropertySpecification properties;
	
	private ForecastInput forecastInput;

	private final boolean longTermUse;

	private final Set<String> testingContext;

	private ModularizationOptions modularizationOptions;

	public Recipe(String orderId, String recipeId, String tag, List<RecipeStep> steps, LinkExchangeModel source, boolean longTermUse, Set<String> testingContext, OrderOptions options,
			ModularizationOptions modularizationOptions, ForecastInput forecastInput) {
		this.orderId = orderId;
		this.recipeId = recipeId;
		this.iterator = steps.listIterator(steps.size());
		this.tag = tag;
		this.source = source;
		this.longTermUse = longTermUse;
		this.testingContext = testingContext;
		this.modularizationOptions = modularizationOptions;
		this.setForecastInput(forecastInput);
		initIterator(source);

		if (options != null) {
			this.properties = options.toProperties();
		}
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

	public String getTag() {
		return tag;
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
		task.setTag(tag);
		task.setSource(source);
		task.setProperties(properties);
		task.setForecastInput(forecastInput);
		task.setLongTermUse(longTermUse);
		task.setModularizationOptions(modularizationOptions);

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
