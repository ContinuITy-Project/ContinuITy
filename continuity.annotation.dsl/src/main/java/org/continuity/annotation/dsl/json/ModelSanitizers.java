package org.continuity.annotation.dsl.json;

import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.DataInput;
import org.continuity.annotation.dsl.ann.Input;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.visitor.ContinuityModelVisitor;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * @author Henning Schulz
 *
 */
public class ModelSanitizers {

	@SuppressWarnings("unchecked")
	private static final Class<ServiceInterface<?>> SERVICE_INTERFACE_CLASS = (Class<ServiceInterface<?>>) (Class<?>) ServiceInterface.class;

	private ModelSanitizers() {
	}

	@SuppressWarnings("unchecked")
	public static <T extends ContinuityModelElement> StdConverter<? super T, ? super T> get(Class<T> type) {
		if (org.continuity.annotation.dsl.ann.SystemAnnotation.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new SystemAnnotation();
		}

		if (org.continuity.annotation.dsl.ann.InterfaceAnnotation.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new InterfaceAnnotation();
		}

		if (org.continuity.annotation.dsl.ann.ParameterAnnotation.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new ParameterAnnotation();
		}

		if (org.continuity.annotation.dsl.ann.RegExExtraction.class.isAssignableFrom(type)) {
			return (StdConverter<? super T, ? super T>) new RegExExtraction();
		}

		return new Noop<>();
	}

	public static boolean isNoop(StdConverter<?, ?> converter) {
		return converter instanceof Noop;
	}

	public static void fixAll(ContinuityModelElement model) {
		ContinuityModelVisitor visitor = new ContinuityModelVisitor(ModelSanitizers::visitElement);
		visitor.visit(model);
	}

	private static <T extends ContinuityModelElement> boolean visitElement(T element) {
		@SuppressWarnings("unchecked")
		StdConverter<? super T, ? super T> converter = (StdConverter<? super T, ? super T>) get(element.getClass());

		if (!isNoop(converter)) {
			converter.convert(element);
		}

		return true;
	}

	// TODO: Move to another class (should not be used in @JsonSerialize annotation)
	public static class SystemAnnotation extends StdConverter<org.continuity.annotation.dsl.ann.SystemAnnotation, org.continuity.annotation.dsl.ann.SystemAnnotation> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.annotation.dsl.ann.SystemAnnotation convert(org.continuity.annotation.dsl.ann.SystemAnnotation annotation) {
			for (Input input : annotation.getInputs()) {
				if (input instanceof DataInput) {
					fixDataInput((DataInput) input);
				}
			}

			return annotation;
		}

		private void fixDataInput(DataInput input) {
			for (org.continuity.annotation.dsl.ann.DataInput ass : input.getAssociated()) {
				if (!ass.getAssociated().contains(input)) {
					ass.getAssociated().add(input);
					System.out.println();
				}
			}
		}
	}

	public static class InterfaceAnnotation extends StdConverter<org.continuity.annotation.dsl.ann.InterfaceAnnotation, org.continuity.annotation.dsl.ann.InterfaceAnnotation> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.annotation.dsl.ann.InterfaceAnnotation convert(org.continuity.annotation.dsl.ann.InterfaceAnnotation annotation) {
			WeakReference<ServiceInterface<?>> ref = annotation.getAnnotatedInterface();
			annotation.setAnnotatedInterface(WeakReference.create(SERVICE_INTERFACE_CLASS, ref.getId()));
			return annotation;
		}
	}

	public static class ParameterAnnotation extends StdConverter<org.continuity.annotation.dsl.ann.ParameterAnnotation, org.continuity.annotation.dsl.ann.ParameterAnnotation> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.annotation.dsl.ann.ParameterAnnotation convert(org.continuity.annotation.dsl.ann.ParameterAnnotation annotation) {
			WeakReference<Parameter> ref = annotation.getAnnotatedParameter();
			annotation.setAnnotatedParameter(WeakReference.create(Parameter.class, ref.getId()));
			return annotation;
		}
	}

	public static class RegExExtraction extends StdConverter<org.continuity.annotation.dsl.ann.RegExExtraction, org.continuity.annotation.dsl.ann.RegExExtraction> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public org.continuity.annotation.dsl.ann.RegExExtraction convert(org.continuity.annotation.dsl.ann.RegExExtraction extraction) {
			WeakReference<ServiceInterface<?>> ref = extraction.getFrom();
			extraction.setFrom(WeakReference.create(SERVICE_INTERFACE_CLASS, ref.getId()));
			return extraction;
		}
	}

	private static class Noop<T extends ContinuityModelElement> extends StdConverter<T, T> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T convert(T value) {
			return value;
		}
	}

}
