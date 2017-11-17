package org.continuity.jmeter.entities;

import java.util.Map;

import org.apache.jorphan.collections.ListedHashTree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Henning Schulz
 *
 */
public class TestPlanPack {

	@JsonProperty("test-plan")
	@JsonDeserialize(using = TestPlanDeserializer.class)
	private ListedHashTree testPlan;

	private Map<String, String[][]> behaviors;

	/**
	 * The link to the annotation model ({@code annotationLink + "/annotation"} holds the
	 * annotation, {@code annotationLink + "/system"} holds the system model).
	 */
	@JsonProperty("annotation-link")
	private String annotationLink;

	/**
	 *
	 */
	public TestPlanPack() {
	}

	/**
	 * @param testPlan
	 * @param behaviors
	 */
	public TestPlanPack(ListedHashTree testPlan, Map<String, String[][]> behaviors) {
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

	/**
	 * Gets {@link #annotationLink}.
	 *
	 * @return {@link #annotationLink}
	 */
	public String getAnnotationLink() {
		return this.annotationLink;
	}

	/**
	 * Sets {@link #annotationLink}.
	 *
	 * @param annotationLink
	 *            New value for {@link #annotationLink}
	 */
	public void setAnnotationLink(String annotationLink) {
		this.annotationLink = annotationLink;
	}

}
