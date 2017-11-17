package org.continuity.wessbas.transform.jmeter;

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
		converter.convertToLoadTest(WessbasDslInstance.DVDSTORE_PARSED.get());
	}

}
