package org.continuity.wessbas.transform.annotation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.continuity.annotation.dsl.ann.Input;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.commons.workload.dsl.AnnotationExtractor;

import m4jdsl.WorkloadModel;

/**
 * Transforms the WESSBAS DSL into the ContinuITy workload DSL.
 *
 * @author Henning Schulz
 *
 */
public class DslFromWessbasExtractor {

	private static final String SYSTEM_UNKNOWN = "UNKNOWN";

	private WorkloadModel wessbasModel;

	private String systemName = SYSTEM_UNKNOWN;

	private SystemModel extractedSystem;

	private SystemAnnotation extractedAnnotation;

	private List<Pair<Input, Parameter>> extractedInputs;

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
		this.extractedSystem = null;
		this.extractedAnnotation = null;

		this.wessbasModel = wessbasModel;

		if (systemName != null) {
			this.systemName = systemName;
		}
	}

	/**
	 * Transforms the workload model into a System model.
	 *
	 * @return The transformed system model.
	 */
	public SystemModel extractSystemModel() {
		if (extractedSystem != null) {
			return extractedSystem;
		}

		extractedSystem = new SystemModel();
		extractedSystem.setId(systemName);
		extractedInputs = new ArrayList<>();

		SessionLayerTransformer transformer = new SessionLayerTransformer(wessbasModel.getApplicationModel());
		transformer.registerOnInterfaceFoundListener(extractedSystem::addInterface);
		transformer.registerOnInputFoundListener(extractedInputs::add);
		transformer.transform();

		extractedAnnotation = new AnnotationExtractor().extractAnnotation(extractedSystem);
		extractedInputs.forEach(pair -> handleInput(pair.getFirst(), pair.getSecond()));

		return extractedSystem;
	}

	private void handleInput(Input input, Parameter parameter) {
		ParameterAnnotationHolder targetHolder = new ParameterAnnotationHolder();
		ContinuityByClassSearcher<ParameterAnnotation> searcher = new ContinuityByClassSearcher<>(ParameterAnnotation.class, a -> checkParamAnnotation(a, parameter, targetHolder));
		searcher.visit(extractedAnnotation);

		ParameterAnnotation ann = targetHolder.annotation;
		ann.setInput(input);
		extractedAnnotation.addInput(input);
	}

	private boolean checkParamAnnotation(ParameterAnnotation ann, Parameter param, ParameterAnnotationHolder targetHolder) {
		if (ann.getAnnotatedParameter().getId().equals(param.getId())) {
			targetHolder.annotation = ann;
		}

		return false;
	}

	/**
	 * Generates an initial annotation that can be changed by users.
	 *
	 * @return The transformed annotation model.
	 */
	public SystemAnnotation extractInitialAnnotation() {
		if (extractedAnnotation != null) {
			return extractedAnnotation;
		}

		if (extractedSystem == null) {
			extractSystemModel();
		}

		return extractedAnnotation;
	}

	private static class ParameterAnnotationHolder {
		private ParameterAnnotation annotation;
	}

}
