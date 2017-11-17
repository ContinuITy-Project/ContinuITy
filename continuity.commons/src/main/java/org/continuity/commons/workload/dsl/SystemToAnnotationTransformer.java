package org.continuity.commons.workload.dsl;

import java.util.Collections;

import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.visitor.ContinuityModelVisitor;
import org.continuity.utils.enums.EnumForClassHolder;

/**
 * Helper for extracting annotations in a {@link ContinuityModelVisitor}.
 *
 * @author Henning Schulz
 *
 */
public class SystemToAnnotationTransformer {

	private SystemAnnotation extractedAnnotation = new SystemAnnotation();

	/**
	 * To be called when a model element is visited.
	 *
	 * @param element
	 *            The visited element
	 * @return Always {@code true} (we do not want to stop).
	 */
	public boolean onModelElementVisited(ContinuityModelElement element) {
		ElementHandler.get(element.getClass()).handleElement(element, extractedAnnotation);
		return true;
	}

	/**
	 * Gets the extracted annotation.
	 *
	 * @return The extracted annotation.
	 */
	public SystemAnnotation getExtractedAnnotation() {
		return extractedAnnotation;
	}

	private enum ElementHandler {

		INTERFACE(ServiceInterface.class) {
			@Override
			public void handleElement(ContinuityModelElement element, SystemAnnotation annotation) {
				ServiceInterface<?> interf = (ServiceInterface<?>) element;
				InterfaceAnnotation ann = new InterfaceAnnotation();
				ann.setAnnotatedInterface(WeakReference.create(interf));

				for (Parameter param : interf.getParameters()) {
					ParameterAnnotation paramAnn = new ParameterAnnotation();
					paramAnn.setAnnotatedParameter(WeakReference.create(param));
					ann.addParameterAnnotation(paramAnn);
				}

				annotation.getInterfaceAnnotations().add(ann);
			}
		},

		NOOP(null) {
			@Override
			public void handleElement(ContinuityModelElement element, SystemAnnotation annotation) {
				// do nothing
			}

		};

		private static final EnumForClassHolder<ElementHandler, ContinuityModelElement> holder = new EnumForClassHolder<>(ElementHandler.class, e -> Collections.singletonList(e.elementType), NOOP);

		private final Class<? extends ContinuityModelElement> elementType;

		private ElementHandler(Class<? extends ContinuityModelElement> elementType) {
			this.elementType = elementType;
		}

		public abstract void handleElement(ContinuityModelElement element, SystemAnnotation annotation);

		public static ElementHandler get(Class<? extends ContinuityModelElement> elementType) {
			return holder.getOne(elementType);
		}
	}

}
