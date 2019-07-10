package org.continuity.api.amqp;

import java.text.ParseException;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.idpa.VersionOrTimestamp;

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
	 * Use an app-id as routing key.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class AppId implements RoutingKeyFormatter {

		public static AppId INSTANCE = new AppId();

		private AppId() {
		}

		/**
		 * Use an app-id as routing key.
		 *
		 * @param appId
		 *            The app-id.
		 * @return The formatted routing key.
		 */
		public String of(org.continuity.idpa.AppId appId) {
			return appId.toString();
		}

	}

	/**
	 * Use an app-id and a version as routing key.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class AppIdAndVersion implements RoutingKeyFormatter {

		public static AppIdAndVersion INSTANCE = new AppIdAndVersion();

		private AppIdAndVersion() {
		}

		/**
		 * Use an app-id and a version as routing key.
		 *
		 * @param appId
		 *            The app-id.
		 * @param version
		 *            The version.
		 * @return The formatted routing key.
		 */
		public String of(org.continuity.idpa.AppId appId, VersionOrTimestamp version) {
			return appId.toString() + "." + version.toString().replace(".", "_");
		}

		/**
		 * Transforms a routingKey formatted with this formatter back to an app-id and a version.
		 *
		 * @param routingKey
		 *            The routing key.
		 * @return A pair of the app-id and the version.
		 */
		public Pair<org.continuity.idpa.AppId, VersionOrTimestamp> from(String routingKey) {
			int index = routingKey.lastIndexOf(".");
			org.continuity.idpa.AppId aid = org.continuity.idpa.AppId.fromString(routingKey.substring(0, index));
			VersionOrTimestamp version;
			try {
				version = VersionOrTimestamp.fromString(routingKey.substring(index + 1).replace("_", "."));
			} catch (NumberFormatException | ParseException e) {
				e.printStackTrace();
				version = null;
			}

			return Pair.of(aid, version);
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

	/**
	 * Use the recipe ID as routing key.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class RecipeId implements RoutingKeyFormatter {

		public static RecipeId INSTANCE = new RecipeId();

		private RecipeId() {
		}

		/**
		 * Use the recipe ID as routing key.
		 *
		 * @param recipeId
		 *            The recipe ID.
		 * @return The formatted recipe ID.
		 */
		public String of(String recipeId) {
			return recipeId;
		}

	}

}
