package org.continuity.orchestrator.entities;

import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;

public interface RecipeStep {

	void setTask(TaskDescription task);

	String getName();

	/**
	 * Determines whether the required data is already available.
	 *
	 * @param source
	 *            The data to be checked.
	 *
	 * @return {@code true} if the data is available or {@code false}, otherwise.
	 */
	boolean checkData(LinkExchangeModel source);

	void execute();

}
