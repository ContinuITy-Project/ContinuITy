package org.continuity.orchestrator.entities;

import org.continuity.api.entities.config.TaskDescription;

public interface RecipeStep {

	void setTask(TaskDescription task);

	String getName();

	void execute();

}
