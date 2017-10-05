package org.continuity.workload.dsl.annotation.visitor;

import java.util.function.Predicate;

import org.continuity.workload.dsl.annotation.AnnotationElement;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationVisitor {

	private final Predicate<AnnotationElement> operation;

	/**
	 *
	 */
	public AnnotationVisitor(Predicate<AnnotationElement> operation) {
		this.operation = operation;
	}

	public void visit(AnnotationElement element) {
		if (operation.test(element)) {
			NestedElementExtractor extractor = NestedElementExtractor.forType(element.getClass());
			for (AnnotationElement nested : extractor.getNestedElements(element)) {
				visit(nested);
			}
		}
	}

}
