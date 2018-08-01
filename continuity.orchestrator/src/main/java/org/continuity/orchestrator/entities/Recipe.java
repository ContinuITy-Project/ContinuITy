package org.continuity.orchestrator.entities;

import java.util.Iterator;

import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.config.PropertySpecification;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskReport;

public class Recipe {

	private final String recipeId;

	private final Iterator<RecipeStep> iterator;

	private int stepCounter = 1;

	private String tag;

	private LinkExchangeModel source;

	private PropertySpecification properties;

	public Recipe(String recipeId, Iterable<RecipeStep> steps, Order order) {
		this.recipeId = recipeId;
		this.iterator = steps.iterator();
		this.tag = order.getTag();
		this.source = order.getSource();

		if (order.getOptions() != null) {
			this.properties = order.getOptions().toProperties();
		}
	}

	public String getRecipeId() {
		return recipeId;
	}

	public LinkExchangeModel getSource() {
		return source;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public RecipeStep next() {
		RecipeStep nextStep = iterator.next();

		String taskId = recipeId + "." + stepCounter++ + "-" + nextStep.getName();
		TaskDescription task = new TaskDescription();
		task.setTaskId(taskId);
		task.setTag(tag);
		task.setSource(source);
		task.setProperties(properties);

		nextStep.setTask(task);

		return nextStep;
	}

	public void updateFromReport(TaskReport report) {
		source.merge(report.getResult());
	}

}
