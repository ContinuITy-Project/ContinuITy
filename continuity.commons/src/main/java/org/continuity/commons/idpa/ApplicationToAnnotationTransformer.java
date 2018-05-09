package org.continuity.commons.idpa;

import java.util.Collections;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.visitor.IdpaVisitor;
import org.continuity.utils.enums.EnumForClassHolder;

/**
 * Helper for extracting annotations in a {@link IdpaVisitor}.
 *
 * @author Henning Schulz
 *
 */
public class ApplicationToAnnotationTransformer {

	private ApplicationAnnotation extractedAnnotation = new ApplicationAnnotation();

	/**
	 * To be called when a model element is visited.
	 *
	 * @param element
	 *            The visited element
	 * @return Always {@code true} (we do not want to stop).
	 */
	public boolean onModelElementVisited(IdpaElement element) {
		ElementHandler.get(element.getClass()).handleElement(element, extractedAnnotation);
		return true;
	}

	/**
	 * Gets the extracted annotation.
	 *
	 * @return The extracted annotation.
	 */
	public ApplicationAnnotation getExtractedAnnotation() {
		return extractedAnnotation;
	}

	private enum ElementHandler {

		INTERFACE(Endpoint.class) {
			@Override
			public void handleElement(IdpaElement element, ApplicationAnnotation annotation) {
				Endpoint<?> interf = (Endpoint<?>) element;
				EndpointAnnotation ann = new EndpointAnnotation();
				ann.setAnnotatedEndpoint(WeakReference.create(interf));

				for (Parameter param : interf.getParameters()) {
					ParameterAnnotation paramAnn = new ParameterAnnotation();
					paramAnn.setAnnotatedParameter(WeakReference.create(param));
					ann.addParameterAnnotation(paramAnn);
				}

				annotation.getEndpointAnnotations().add(ann);
			}
		},

		NOOP(null) {
			@Override
			public void handleElement(IdpaElement element, ApplicationAnnotation annotation) {
				// do nothing
			}

		};

		private static final EnumForClassHolder<ElementHandler, IdpaElement> holder = new EnumForClassHolder<>(ElementHandler.class, e -> Collections.singletonList(e.elementType), NOOP);

		private final Class<? extends IdpaElement> elementType;

		private ElementHandler(Class<? extends IdpaElement> elementType) {
			this.elementType = elementType;
		}

		public abstract void handleElement(IdpaElement element, ApplicationAnnotation annotation);

		public static ElementHandler get(Class<? extends IdpaElement> elementType) {
			return holder.getOne(elementType);
		}
	}

}
