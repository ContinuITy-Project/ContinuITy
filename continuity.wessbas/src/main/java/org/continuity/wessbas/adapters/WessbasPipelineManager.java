package org.continuity.wessbas.adapters;

import java.util.function.Consumer;

import org.continuity.wessbas.entities.WessbasDslInstance;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
public class WessbasPipelineManager {

	private final Consumer<WorkloadModel> onModelCreatedCallback;

	/**
	 * @param onModelCreatedCallback
	 */
	public WessbasPipelineManager(Consumer<WorkloadModel> onModelCreatedCallback) {
		this.onModelCreatedCallback = onModelCreatedCallback;
	}

	/**
	 * Runs the pipeline.
	 *
	 * TODO: Implement TODO: Define sensible input data
	 */
	public void runPipeline() {
		onModelCreatedCallback.accept(WessbasDslInstance.DVDSTORE_PARSED.get());
	}

}
