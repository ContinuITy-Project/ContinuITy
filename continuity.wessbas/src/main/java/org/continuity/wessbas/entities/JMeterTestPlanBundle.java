package org.continuity.wessbas.entities;

import java.util.Map;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.commons.jmeter.JMeterTestPlanSerializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Henning Schulz
 *
 */
public class JMeterTestPlanBundle {

	@JsonProperty("test-plan")
	@JsonSerialize(using = JMeterTestPlanSerializer.class)
	private ListedHashTree testPlan;

	private Map<String, String[][]> behaviors;

	/**
	 *
	 */
	public JMeterTestPlanBundle() {
	}

	/**
	 * @param testPlan
	 * @param behaviors
	 */
	public JMeterTestPlanBundle(ListedHashTree testPlan, Map<String, String[][]> behaviors) {
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
