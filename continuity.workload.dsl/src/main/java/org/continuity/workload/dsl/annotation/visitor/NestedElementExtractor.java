package org.continuity.workload.dsl.annotation.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.continuity.workload.dsl.annotation.AnnotationElement;
import org.continuity.workload.dsl.annotation.DataInput;
import org.continuity.workload.dsl.annotation.ExtractedInput;
import org.continuity.workload.dsl.annotation.InterfaceAnnotation;
import org.continuity.workload.dsl.annotation.ParameterAnnotation;
import org.continuity.workload.dsl.annotation.RegExExtraction;
import org.continuity.workload.dsl.annotation.SystemAnnotation;

/**
 * Utility for extracting the nested elements of {@link AnnotationElement}s.
 *
 * @author Henning Schulz
 *
 */
public enum NestedElementExtractor {

	/**
	 * For {@link SystemAnnotation}.
	 */
	SYSTEM_ANNOTATION(SystemAnnotation.class) {
		@Override
		protected Collection<AnnotationElement> extractNestedElements(AnnotationElement element) {
			SystemAnnotation annotation = (SystemAnnotation) element;
			List<AnnotationElement> join = new ArrayList<>(annotation.getInputs().size() + annotation.getInterfaceAnnotations().size());
			join.addAll(annotation.getInputs());
			join.addAll(annotation.getInterfaceAnnotations());
			return join;
		}
	},

	/**
	 * For {@link InterfaceAnnotation}.
	 */
	INTERFACE_ANNOTATION(InterfaceAnnotation.class) {
		@Override
		protected Collection<AnnotationElement> extractNestedElements(AnnotationElement element) {
			InterfaceAnnotation annotation = (InterfaceAnnotation) element;
			return new ArrayList<>(annotation.getParameterAnnotations());
		}
	},

	/**
	 * For {@link ExtractedInput}.
	 */
	EXTRACTED_INPUT(ExtractedInput.class) {
		@Override
		protected Collection<AnnotationElement> extractNestedElements(AnnotationElement element) {
			ExtractedInput input = (ExtractedInput) element;
			return new ArrayList<>(input.getExtractions());
		}
	},

	/**
	 * For all other elements that do not have nested elements.
	 */
	EMPTY(ParameterAnnotation.class, DataInput.class, RegExExtraction.class) {
		@Override
		protected Collection<AnnotationElement> extractNestedElements(AnnotationElement element) {
			return Collections.emptyList();
		}
	};

	private static final Map<Class<? extends AnnotationElement>, NestedElementExtractor> extractorPerType;

	static {
		extractorPerType = new HashMap<>();

		for (NestedElementExtractor extractor : values()) {
			for (Class<? extends AnnotationElement> type : extractor.types) {
				extractorPerType.put(type, extractor);
			}
		}
	}

	private final Class<? extends AnnotationElement>[] types;

	@SafeVarargs
	private NestedElementExtractor(Class<? extends AnnotationElement>... types) {
		this.types = types;
	}

	/**
	 * Returns all nested elements of the passed one. With throw an {@link IllegalArgumentException}
	 * if the class of the passed element does not match.
	 *
	 * @param element
	 *            The element whose nested elements should be returned.
	 * @return The nested elements of {@code element}.
	 */
	public Collection<AnnotationElement> getNestedElements(AnnotationElement element) {
		boolean assignable = false;
		for (Class<? extends AnnotationElement> type : types) {
			if (type.isAssignableFrom(element.getClass())) {
				assignable = true;
				break;
			}
		}

		if (!assignable) {
			throw new IllegalArgumentException(toString() + " cannot process " + element.getClass());
		}

		return extractNestedElements(element);
	}

	protected abstract Collection<AnnotationElement> extractNestedElements(AnnotationElement element);

	/**
	 * Returns the extractor for the passed type.
	 *
	 * @param type
	 *            The class of the {@link AnnotationElement} to be processed.
	 * @return A NestedElementExtractor that is able to process the passed type.
	 */
	public static NestedElementExtractor forType(Class<? extends AnnotationElement> type) {
		NestedElementExtractor extractor = extractorPerType.get(type);

		if (extractor == null) {
			for (Entry<Class<? extends AnnotationElement>, NestedElementExtractor> entry : extractorPerType.entrySet()) {
				if (entry.getKey().isAssignableFrom(type)) {
					return entry.getValue();
				}
			}
		}

		return null;
	}

}
