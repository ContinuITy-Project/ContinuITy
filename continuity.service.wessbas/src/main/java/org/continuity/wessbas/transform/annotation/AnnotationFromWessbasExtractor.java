package org.continuity.wessbas.transform.annotation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

import m4jdsl.WorkloadModel;

/**
 * Transforms the WESSBAS DSL into the ContinuITy annotation DSL.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationFromWessbasExtractor {

	private static final String SYSTEM_UNKNOWN = "UNKNOWN";

	private WorkloadModel wessbasModel;

	private String systemName = SYSTEM_UNKNOWN;

	private Application extractedSystem;

	private ApplicationAnnotation extractedAnnotation;

	private List<Pair<Input, Parameter>> extractedInputs;

	/**
	 * Constructor. Requires calling {@link AnnotationFromWessbasExtractor#init(WorkloadModel)
	 * init(WorkloadModel)} before executing the transformation.
	 */
	public AnnotationFromWessbasExtractor() {
	}

	/**
	 * Constructor. Directly sets the WESSBAS DSL instance to be transformed.
	 *
	 * @param wessbasModel
	 *            The WESSBAS DSL instance to be transformed.
	 */
	public AnnotationFromWessbasExtractor(WorkloadModel wessbasModel) {
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
	public AnnotationFromWessbasExtractor(WorkloadModel wessbasModel, String systemName) {
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
	public Application extractSystemModel() {
		if (extractedSystem != null) {
			return extractedSystem;
		}

		extractedSystem = new Application();
		extractedSystem.setId(systemName);
		extractedInputs = new ArrayList<>();

		SessionLayerTransformer transformer = new SessionLayerTransformer(wessbasModel.getApplicationModel());
		transformer.registerOnInterfaceFoundListener(extractedSystem::addEndpoint);
		transformer.registerOnInputFoundListener(extractedInputs::add);
		transformer.transform();

		extractedAnnotation = new AnnotationExtractor().extractAnnotation(extractedSystem);
		extractedInputs.forEach(pair -> handleInput(pair.getFirst(), pair.getSecond()));

		return extractedSystem;
	}

	private void handleInput(Input input, Parameter parameter) {
		ParameterAnnotationHolder targetHolder = new ParameterAnnotationHolder();
		IdpaByClassSearcher<ParameterAnnotation> searcher = new IdpaByClassSearcher<>(ParameterAnnotation.class, a -> checkParamAnnotation(a, parameter, targetHolder));
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
	public ApplicationAnnotation extractInitialAnnotation() {
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
