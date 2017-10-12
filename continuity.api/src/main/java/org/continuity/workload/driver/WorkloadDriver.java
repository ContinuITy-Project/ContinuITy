package org.continuity.workload.driver;

/**
 * A WorkloadDriver is responsible for executing a load test.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            Type of the load test.
 */
public interface WorkloadDriver<T> {

	/**
	 * Executes the passed load test.
	 *
	 * @param loadTest
	 *            Test to be executed.
	 */
	void executeWorkload(T loadTest);

}
