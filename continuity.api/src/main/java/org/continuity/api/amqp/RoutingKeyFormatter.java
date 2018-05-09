package org.continuity.api.amqp;

/**
 * A formatter for AMQP routing keys.
 *
 * @author Henning Schulz
 *
 */
public interface RoutingKeyFormatter {

	/**
	 * Use a keyword as routing key, e.g., {@code report}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Keyword implements RoutingKeyFormatter {

		public static Keyword INSTANCE = new Keyword();

		private Keyword() {
		}

		/**
		 * Use a keyword as routing key, e.g., {@code report}.
		 *
		 * @param keyword
		 *            The keyword.
		 * @return The formatted routing key.
		 */
		public String of(String keyword) {
			return keyword;
		}

	}

	/**
	 * Use a tag as routing key.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Tag implements RoutingKeyFormatter {

		public static Tag INSTANCE = new Tag();

		private Tag() {
		}

		/**
		 * Use a tag as routing key.
		 *
		 * @param tag
		 *            The tag.
		 * @return The formatted routing key.
		 */
		public String of(String tag) {
			return tag;
		}

	}

	/**
	 * Use a workload type as routing key, e.g., {@code wessbas}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class WorkloadType implements RoutingKeyFormatter {

		public static WorkloadType INSTANCE = new WorkloadType();

		private WorkloadType() {
		}

		/**
		 * Use a workload type as routing key, e.g., {@code wessbas}.
		 *
		 * @param workloadType
		 *            The workload type.
		 * @return The formatted routing key.
		 */
		public String of(String workloadType) {
			return workloadType;
		}

	}

	/**
	 * Use a load test type as routing key, e.g., {@code jmeter}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class LoadTestType implements RoutingKeyFormatter {

		public static LoadTestType INSTANCE = new LoadTestType();

		private LoadTestType() {
		}

		/**
		 * Use a load test type as routing key, e.g., {@code jmeter}.
		 *
		 * @param loadTestType
		 *            The load test type.
		 * @return The formatted routing key.
		 */
		public String of(String loadTestType) {
			return loadTestType;
		}

	}

	/**
	 * Use a workload type and a load test type as routing key, e.g., {@code wessbas} and
	 * {@code jmeter}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class WorkloadAndLoadTestType implements RoutingKeyFormatter {

		public static WorkloadAndLoadTestType INSTANCE = new WorkloadAndLoadTestType();

		private WorkloadAndLoadTestType() {
		}

		/**
		 * Use a workload type and a load test type as routing key, e.g., {@code wessbas} and
		 * {@code jmeter}.
		 *
		 * @param workloadType
		 *            The workload type.
		 * @param loadTestType
		 *            The load test type.
		 * @return The formatted routing key.
		 */
		public String of(String workloadType, String loadTestType) {
			return workloadType + "." + loadTestType;
		}

	}

	/**
	 * Use a workload type and a workload link as routing key, e.g., {@code wessbas} and
	 * {@code wessbas/model/my-app-1}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class WorkloadTypeAndLink implements RoutingKeyFormatter {

		public static WorkloadTypeAndLink INSTANCE = new WorkloadTypeAndLink();

		private WorkloadTypeAndLink() {
		}

		/**
		 * Use a workload type and a workload link as routing key, e.g., {@code wessbas} and
		 * {@code wessbas/model/my-app-1}.
		 *
		 * @param workloadType
		 *            The workload type.
		 * @param workloadLink
		 *            The workload link.
		 * @return The formatted routing key.
		 */
		public String of(String workloadType, String workloadLink) {
			return workloadType + "." + workloadLink;
		}

	}

	/**
	 * Use the service name as routing key, e.g., {@code frontend}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class ServiceName implements RoutingKeyFormatter {

		public static ServiceName INSTANCE = new ServiceName();

		private ServiceName() {
		}

		/**
		 * Use the service name as routing key, e.g., {@code frontend}.
		 *
		 * @param serviceName
		 *            The service name.
		 * @return The formatted routing key.
		 */
		public String of(String serviceName) {
			return serviceName;
		}

	}

}
