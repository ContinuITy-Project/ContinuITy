package org.continuity.wessbas.transform.jmeter;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.wessbas.entities.WessbasBundle;
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

		checkDuration(testPlanBundle.getTestPlan(), 3600);
		checkRampup(testPlanBundle.getTestPlan(), 800); // workload model has 800 users
	}

	@Test
	public void testWithIntensity() {
		WessbasBundle bundle = new WessbasBundle(null, WessbasDslInstance.DVDSTORE_PARSED.get(), "100,150,110", 60000);

		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("configuration");
		JMeterTestPlanBundle testPlanBundle = converter.convertToLoadTest(bundle);

		Assert.assertNotNull("The test plan must not be null", testPlanBundle.getTestPlan());
		Assert.assertNotNull("The behavior matrices must not be null", testPlanBundle.getBehaviors());
		Assert.assertTrue("There should be at least one behavior matrix.", testPlanBundle.getBehaviors().size() > 0);

		checkDuration(testPlanBundle.getTestPlan(), 180);
		checkRampup(testPlanBundle.getTestPlan(), 100);
	}

	private void checkDuration(ListedHashTree testPlan, long expected) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		for (ThreadGroup group : search.getSearchResults()) {
			Assert.assertEquals(expected, group.getDuration());

			Controller mainController = group.getSamplerController();

			if (mainController instanceof LoopController) {
				Assert.assertEquals(-1, ((LoopController) mainController).getLoops());
			}
		}
	}

	private void checkRampup(ListedHashTree testPlan, int expected) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		for (ThreadGroup group : search.getSearchResults()) {
			Assert.assertEquals(expected, group.getRampUp());
		}
	}

}
