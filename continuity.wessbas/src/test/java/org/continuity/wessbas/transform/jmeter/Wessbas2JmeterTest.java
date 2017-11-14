package org.continuity.wessbas.transform.jmeter;

import org.continuity.annotation.dsl.custom.CustomAnnotation;
import org.continuity.annotation.dsl.custom.CustomAnnotationElement;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.continuity.commons.exceptions.AnnotationNotSupportedException;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.junit.Test;

/**
 * @author Henning Schulz
 *
 */
public class Wessbas2JmeterTest {

	@Test
	public void test() {
		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("examples/dvdstore");
		converter.convertToLoadTest(WessbasDslInstance.DVDSTORE_PARSED.get(),
				ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation());
	}

	@Test(expected = AnnotationNotSupportedException.class)
	public void testWithExtensions() throws AnnotationNotSupportedException {
		CustomAnnotation extension = new CustomAnnotation();
		CustomAnnotationElement element = new CustomAnnotationElement();
		element.setId("myextension");
		extension.addElement(element);

		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("./");
		converter.convertToLoadTest(WessbasDslInstance.SIMPLE.get(), ContinuityModelTestInstance.SIMPLE.getAnnotation(), extension);
	}

}
