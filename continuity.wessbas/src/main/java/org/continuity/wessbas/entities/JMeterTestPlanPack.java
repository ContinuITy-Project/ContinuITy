package org.continuity.wessbas.entities;

import java.util.Map;

import org.apache.jorphan.collections.ListedHashTree;

/**
 * @author Henning Schulz
 *
 */
public class JMeterTestPlanPack {

	private ListedHashTree testPlan;

	private Map<String, String[][]> behaviors;

	/**
	 *
	 */
	public JMeterTestPlanPack() {
	}

	/**
	 * @param testPlan
	 * @param behaviors
	 */
	public JMeterTestPlanPack(ListedHashTree testPlan, Map<String, String[][]> behaviors) {
		this.testPlan = testPlan;
		this.behaviors = behaviors;
	}

	/**
	 * Gets {@link #testPlan}.
	 * 
	 * @return {@link #testPlan}
	 */
	public ListedHashTree getTestPlan() {
		return this.testPlan;
	}

	/**
	 * Sets {@link #testPlan}.
	 * 
	 * @param testPlan
	 *            New value for {@link #testPlan}
	 */
	public void setTestPlan(ListedHashTree testPlan) {
		this.testPlan = testPlan;
	}

	/**
	 * Gets {@link #behaviors}.
	 * 
	 * @return {@link #behaviors}
	 */
	public Map<String, String[][]> getBehaviors() {
		return this.behaviors;
	}

	/**
	 * Sets {@link #behaviors}.
	 * 
	 * @param behaviors
	 *            New value for {@link #behaviors}
	 */
	public void setBehaviors(Map<String, String[][]> behaviors) {
		this.behaviors = behaviors;
	}

}
