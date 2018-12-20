package org.continuity.idpa.serialization;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ListInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.visitor.IdpaVisitor;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * @author Henning Schulz
 *
 */
public class ModelSanitizers {

	@SuppressWarnings("unchecked")
	private static final Class<Endpoint<?>> SERVICE_INTERFACE_CLASS = (Class<Endpoint<?>>) (Class<?>) Endpoint.class;

	private ModelSanitizers() {
	}

	@SuppressWarnings("unchecked")
	public static <T extends IdpaElement> StdConverter<? super T, ? super T> get(Class<T> type) {
		if (org.continuity.idpa.annotation.ApplicationAnnotation.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new SystemAnnotation();
		}

		if (org.continuity.idpa.annotation.EndpointAnnotation.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new InterfaceAnnotation();
		}

		if (org.continuity.idpa.annotation.ParameterAnnotation.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new ParameterAnnotation();
		}

		if (org.continuity.idpa.annotation.RegExExtraction.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new RegExExtraction();
		}

		return new Noop<>();
	}

	public static boolean isNoop(StdConverter<?, ?> converter) {
		return converter instanceof Noop;
	}

	public static void fixAll(IdpaElement model) {
		IdpaVisitor visitor = new IdpaVisitor(ModelSanitizers::visitElement);
		visitor.visit(model);
	}

	private static <T extends IdpaElement> boolean visitElement(T element) {
		@SuppressWarnings("unchecked")
		StdConverter<? super T, ? super T> converter = (StdConverter<? super T, ? super T>) get(element.getClass());

		if (!isNoop(converter)) {
			converter.convert(element);
		}

		return true;
	}

	// TODO: Move to another class (should not be used in @JsonSerialize annotation)
	public static class SystemAnnotation extends StdConverter<org.continuity.idpa.annotation.ApplicationAnnotation, org.continuity.idpa.annotation.ApplicationAnnotation> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.idpa.annotation.ApplicationAnnotation convert(org.continuity.idpa.annotation.ApplicationAnnotation annotation) {
			for (Input input : annotation.getInputs()) {
				if (input instanceof ListInput) {
					fixDataInput((ListInput) input);
				}
			}

			return annotation;
		}

		private void fixDataInput(ListInput input) {
			for (org.continuity.idpa.annotation.ListInput ass : input.getAssociated()) {
				if (!ass.getAssociated().contains(input)) {
					ass.getAssociated().add(input);
				}
			}
		}
	}

	public static class InterfaceAnnotation extends StdConverter<org.continuity.idpa.annotation.EndpointAnnotation, org.continuity.idpa.annotation.EndpointAnnotation> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.idpa.annotation.EndpointAnnotation convert(org.continuity.idpa.annotation.EndpointAnnotation annotation) {
			WeakReference<Endpoint<?>> ref = annotation.getAnnotatedEndpoint();
			annotation.setAnnotatedEndpoint(WeakReference.create(SERVICE_INTERFACE_CLASS, ref.getId()));
			return annotation;
		}
	}

	public static class ParameterAnnotation extends StdConverter<org.continuity.idpa.annotation.ParameterAnnotation, org.continuity.idpa.annotation.ParameterAnnotation> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.idpa.annotation.ParameterAnnotation convert(org.continuity.idpa.annotation.ParameterAnnotation annotation) {
			WeakReference<Parameter> ref = annotation.getAnnotatedParameter();
			annotation.setAnnotatedParameter(WeakReference.create(Parameter.class, ref.getId()));
			return annotation;
		}
	}

	public static class RegExExtraction extends StdConverter<org.continuity.idpa.annotation.RegExExtraction, org.continuity.idpa.annotation.RegExExtraction> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.idpa.annotation.RegExExtraction convert(org.continuity.idpa.annotation.RegExExtraction extraction) {
			WeakReference<Endpoint<?>> ref = extraction.getFrom();
			extraction.setFrom(WeakReference.create(SERVICE_INTERFACE_CLASS, ref.getId()));
			return extraction;
		}
	}

	private static class Noop<T extends IdpaElement> extends StdConverter<T, T> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T convert(T value) {
			return value;
		}
	}

}
