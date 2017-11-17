package org.continuity.wessbas.managers;

import java.util.function.Consumer;

import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.entities.WessbasDslInstance;

import m4jdsl.WorkloadModel;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL instance.
 *
 * @author Henning Schulz
 *
 */
public class WessbasPipelineManager {

	private final Consumer<WorkloadModel> onModelCreatedCallback;

	/**
	 * Constructor.
	 *
	 * @param onModelCreatedCallback
	 *            The function to be called when the model was created.
	 */
	public WessbasPipelineManager(Consumer<WorkloadModel> onModelCreatedCallback) {
		this.onModelCreatedCallback = onModelCreatedCallback;
	}

	/**
	 * Runs the pipeline and calls the callback when the model was created.
	 *
	 * TODO: Implement
	 *
	 * @param data
	 *            Input monitoring data to be transformed into a WESSBAS DSL instance.
	 */
	public void runPipeline(MonitoringData data) {
		onModelCreatedCallback.accept(WessbasDslInstance.DVDSTORE_PARSED.get());
	}

}
