package org.continuity.workload.dsl.yaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.continuity.utils.enums.EnumForClassHolder;
import org.continuity.workload.dsl.ContinuityModelElement;
import org.continuity.workload.dsl.WeakReference;
import org.continuity.workload.dsl.annotation.DataInput;
import org.continuity.workload.dsl.annotation.InterfaceAnnotation;
import org.continuity.workload.dsl.annotation.ParameterAnnotation;
import org.continuity.workload.dsl.annotation.RegExExtraction;
import org.continuity.workload.dsl.system.Parameter;
import org.continuity.workload.dsl.system.ServiceInterface;
import org.continuity.workload.dsl.visitor.ContinuityModelVisitor;

/**
 * @author Henning Schulz
 *
 */
public enum ModelValidator {

	ASSOCIATED(DataInput.class) {
		@Override
		protected void fixChecked(ContinuityModelElement element) {
			DataInput input = (DataInput) element;
			for (DataInput ass : input.getAssociated()) {
				if (!ass.getAssociated().contains(input)) {
					ass.getAssociated().add(input);
				}
			}
		}
	},

	WEAK_REFERENCES(InterfaceAnnotation.class, ParameterAnnotation.class, RegExExtraction.class) {
		@SuppressWarnings("unchecked")
		@Override
		protected void fixChecked(ContinuityModelElement element) {
			Class<?> clazz = ServiceInterface.class;
			Class<ServiceInterface<?>> serviceInterfaceClass = (Class<ServiceInterface<?>>) clazz;

			if (element instanceof InterfaceAnnotation) {
				WeakReference<ServiceInterface<?>> ref = ((InterfaceAnnotation) element).getAnnotatedInterface();
				((InterfaceAnnotation) element).setAnnotatedInterface(WeakReference.create(serviceInterfaceClass, ref.getId()));
			} else if (element instanceof ParameterAnnotation) {
				WeakReference<Parameter> ref = ((ParameterAnnotation) element).getAnnotatedParameter();
				((ParameterAnnotation) element).setAnnotatedParameter(WeakReference.create(Parameter.class, ref.getId()));
			} else if (element instanceof RegExExtraction) {
				WeakReference<ServiceInterface<?>> ref = ((RegExExtraction) element).getFrom();
				((RegExExtraction) element).setFrom(WeakReference.create(serviceInterfaceClass, ref.getId()));
			}
		}
	},

	NOOP {
		@Override
		protected void fixChecked(ContinuityModelElement element) {
		}
	};

	private static final EnumForClassHolder<ModelValidator, ContinuityModelElement> holder = new EnumForClassHolder<>(ModelValidator.class, ModelValidator::getElementTypes, NOOP);

	private final List<Class<? extends ContinuityModelElement>> elementTypes;

	/**
	 *
	 */
	@SafeVarargs
	private ModelValidator(Class<? extends ContinuityModelElement>... elementTypes) {
		this.elementTypes = Arrays.asList(elementTypes);
	}

	public void fix(ContinuityModelElement element) {
		if (element == null) {
			throw new IllegalArgumentException("Cannot process null element!");
		}

		if (elementTypes.contains(element.getClass())) {
			fixChecked(element);
		} else {
			throw new IllegalArgumentException("Cannot process element of type " + element.getClass().getSimpleName() + "!");
		}
	}

	protected abstract void fixChecked(ContinuityModelElement element);

	public static void fixAll(ContinuityModelElement model) {
		ContinuityModelVisitor visitor = new ContinuityModelVisitor(ModelValidator::visitElement);
		visitor.visit(model);
	}

	private static boolean visitElement(ContinuityModelElement element) {
		for (ModelValidator validator : holder.get(element.getClass())) {
			validator.fixChecked(element);
		}

		return true;
	}

	private Collection<Class<? extends ContinuityModelElement>> getElementTypes() {
		return elementTypes;
	}

}
