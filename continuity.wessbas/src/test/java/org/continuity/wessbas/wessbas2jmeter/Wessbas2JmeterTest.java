package org.continuity.wessbas.wessbas2jmeter;

import org.continuity.wessbas.WessbasDslInstance;
import org.continuity.workload.driver.AnnotationNotSupportedException;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtension;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtensionElement;
import org.continuity.workload.dsl.test.ContinuityModelTestInstance;
import org.junit.Test;

/**
 * @author Henning Schulz
 *
 */
public class Wessbas2JmeterTest {

	@Test
	public void test() {
		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("examples/dvdstore");
		converter.convertToWorkload(WessbasDslInstance.DVDSTORE_PARSED.get(), ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel(),
				ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation());
	}

	@Test(expected = AnnotationNotSupportedException.class)
	public void testWithExtensions() throws AnnotationNotSupportedException {
		AnnotationExtension extension = new AnnotationExtension();
		AnnotationExtensionElement element = new AnnotationExtensionElement();
		element.setId("myextension");
		extension.addElement(element);

		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("./");
		converter.convertToWorkload(WessbasDslInstance.SIMPLE.get(), ContinuityModelTestInstance.SIMPLE.getSystemModel(), ContinuityModelTestInstance.SIMPLE.getAnnotation(), extension);
	}

}
