package org.continuity.idpa.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CombinedInput;
import org.continuity.idpa.annotation.CounterInput;
import org.continuity.idpa.annotation.CsvColumnInput;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.DatetimeInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ListInput;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.RandomNumberInput;
import org.continuity.idpa.annotation.RandomStringInput;
import org.continuity.idpa.annotation.extracted.ExtractedInput;
import org.continuity.idpa.annotation.extracted.JsonPathExtraction;
import org.continuity.idpa.annotation.extracted.RegExExtraction;
import org.continuity.idpa.annotation.json.JsonInput;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Parameter;

/**
 * Utility for extracting the nested elements of {@link IdpaElement}s.
 *
 * @author Henning Schulz
 *
 */
public enum NestedElementExtractor {

	TARGET_SYSTEM(Application.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			Application system = (Application) element;
			return new ArrayList<>(system.getEndpoints());
		}
	},

	INTERFACE(Endpoint.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			Endpoint<?> interf = (Endpoint<?>) element;
			return new ArrayList<>(interf.getParameters());
		}
	},

	/**
	 * For {@link ApplicationAnnotation}.
	 */
	SYSTEM_ANNOTATION(ApplicationAnnotation.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			ApplicationAnnotation annotation = (ApplicationAnnotation) element;
			List<IdpaElement> join = new ArrayList<>(annotation.getInputs().size() + annotation.getEndpointAnnotations().size());
			join.addAll(annotation.getInputs());
			join.addAll(annotation.getEndpointAnnotations());
			return join;
		}
	},

	/**
	 * For {@link EndpointAnnotation}.
	 */
	INTERFACE_ANNOTATION(EndpointAnnotation.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			EndpointAnnotation annotation = (EndpointAnnotation) element;
			return new ArrayList<>(annotation.getParameterAnnotations());
		}
	},

	/**
	 * For {@link ExtractedInput}.
	 */
	EXTRACTED_INPUT(ExtractedInput.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			ExtractedInput input = (ExtractedInput) element;
			return new ArrayList<>(input.getExtractions());
		}
	},

	/**
	 * For {@link CsvInput}
	 */
	CSV_INPUT(CsvInput.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			CsvInput input = (CsvInput) element;

			if (input.getColumns() != null) {
				return new ArrayList<>(input.getColumns());
			} else {
				return Collections.emptyList();
			}
		}
	},

	/**
	 * For all other elements that do not have nested elements.
	 */
	EMPTY(Parameter.class, ParameterAnnotation.class, ListInput.class, CounterInput.class, RegExExtraction.class, JsonPathExtraction.class, JsonInput.class, CsvColumnInput.class,
			CombinedInput.class, DatetimeInput.class, RandomNumberInput.class, RandomStringInput.class) {
		@Override
		protected Collection<IdpaElement> extractNestedElements(IdpaElement element) {
			return Collections.emptyList();
		}
	};

	private static final Map<Class<? extends IdpaElement>, NestedElementExtractor> extractorPerType;

	static {
		extractorPerType = new HashMap<>();

		for (NestedElementExtractor extractor : values()) {
			for (Class<? extends IdpaElement> type : extractor.types) {
				extractorPerType.put(type, extractor);
			}
		}
	}

	private final Class<? extends IdpaElement>[] types;

	@SafeVarargs
	private NestedElementExtractor(Class<? extends IdpaElement>... types) {
		this.types = types;
	}

	/**
	 * Returns all nested elements of the passed one. Will throw an {@link IllegalArgumentException}
	 * if the class of the passed element does not match.
	 *
	 * @param element
	 *            The element whose nested elements should be returned.
	 * @return The nested elements of {@code element}.
	 */
	public Collection<IdpaElement> getNestedElements(IdpaElement element) {
		boolean assignable = false;
		for (Class<? extends IdpaElement> type : types) {
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

	protected abstract Collection<IdpaElement> extractNestedElements(IdpaElement element);

	/**
	 * Returns the extractor for the passed type.
	 *
	 * @param type
	 *            The class of the {@link IdpaElement} to be processed.
	 * @return A NestedElementExtractor that is able to process the passed type.
	 */
	public static NestedElementExtractor forType(Class<? extends IdpaElement> type) {
		NestedElementExtractor extractor = extractorPerType.get(type);

		if (extractor == null) {
			for (Entry<Class<? extends IdpaElement>, NestedElementExtractor> entry : extractorPerType.entrySet()) {
				if (entry.getKey().isAssignableFrom(type)) {
					return entry.getValue();
				}
			}
		}

		return extractor;
	}

}
