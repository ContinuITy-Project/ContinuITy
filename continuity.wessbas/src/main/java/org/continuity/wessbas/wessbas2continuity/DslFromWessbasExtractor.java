package org.continuity.wessbas.wessbas2continuity;

import org.continuity.workload.dsl.DslExtractor;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.TargetSystem;

import m4jdsl.WorkloadModel;

/**
 * Transforms the WESSBAS DSL into the ContinuITy workload DSL.
 *
 * @author Henning Schulz
 *
 */
public class DslFromWessbasExtractor implements DslExtractor {

	private static final String SYSTEM_UNKNOWN = "UNKNOWN";

	private WorkloadModel wessbasModel;

	private String systemName = SYSTEM_UNKNOWN;

	/**
	 * Constructor. Requires calling {@link DslFromWessbasExtractor#init(WorkloadModel)
	 * init(WorkloadModel)} before executing the transformation.
	 */
	public DslFromWessbasExtractor() {
	}

	/**
	 * Constructor. Directly sets the WESSBAS DSL instance to be transformed.
	 *
	 * @param wessbasModel
	 *            The WESSBAS DSL instance to be transformed.
	 */
	public DslFromWessbasExtractor(WorkloadModel wessbasModel) {
		init(wessbasModel, null);
	}

	/**
	 * Constructor. Directly sets the WESSBAS DSL instance to be transformed and the system's name.
	 *
	 * @param wessbasModel
	 *            The WESSBAS DSL instance to be transformed.
	 * @param systemName
	 *            The system's name.
	 */
	public DslFromWessbasExtractor(WorkloadModel wessbasModel, String systemName) {
		init(wessbasModel, systemName);
	}

	/**
	 * Initializes the extractor with a WESSBAS DSL instance.
	 *
	 * @param wessbasModel
	 *            The WESSBAS DSL instance to be transformed.
	 */
	public void init(WorkloadModel wessbasModel, String systemName) {
		this.wessbasModel = wessbasModel;

		if (systemName != null) {
			this.systemName = systemName;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TargetSystem extractSystemModel() {
		TargetSystem system = new TargetSystem();
		system.setName(systemName);

		SessionLayerTransformer transformer = new SessionLayerTransformer(wessbasModel.getApplicationModel());
		transformer.registerOnInterfaceFoundListener(system::addInterface);
		transformer.transform();

		return system;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SystemAnnotation extractInitialAnnotation() {
		// TODO Auto-generated method stub
		return null;
	}

}
