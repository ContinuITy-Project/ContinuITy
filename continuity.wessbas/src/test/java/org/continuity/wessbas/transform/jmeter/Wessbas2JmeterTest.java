package org.continuity.wessbas.transform.jmeter;

import org.continuity.wessbas.entities.JMeterTestPlanBundle;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Henning Schulz
 *
 */
public class Wessbas2JmeterTest {

	@Test
	public void test() {
		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("configuration");
		JMeterTestPlanBundle testPlanBundle = converter.convertToLoadTest(WessbasDslInstance.DVDSTORE_PARSED.get());

		Assert.assertNotNull("The test plan must not be null", testPlanBundle.getTestPlan());
		Assert.assertNotNull("The behavior matrices must not be null", testPlanBundle.getBehaviors());
		Assert.assertTrue("There should be at least one behavior matrix.", testPlanBundle.getBehaviors().size() > 0);
	}

}
