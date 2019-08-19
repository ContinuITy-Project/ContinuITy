package org.continuity.api.amqp;

import org.continuity.api.amqp.RoutingKeyFormatter.AppId;
import org.continuity.api.amqp.RoutingKeyFormatter.AppIdAndVersion;
import org.continuity.api.amqp.RoutingKeyFormatter.RecipeId;
import org.continuity.api.amqp.RoutingKeyFormatter.ServiceName;
import org.continuity.api.amqp.RoutingKeyFormatter.ServiceNameAndTarget;

/**
 * Holds all AMQP exchange definitions of all ContinuITy services.
 *
 * @author Henning Schulz
 *
 */
public class AmqpApi {

	public static final ExchangeDefinition<ServiceName> DEAD_LETTER_EXCHANGE = ExchangeDefinition.event("global", "deadletter").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

	public static final String DEAD_LETTER_EXCHANGE_KEY = "x-dead-letter-exchange";

	public static final String DEAD_LETTER_ROUTING_KEY_KEY = "x-dead-letter-routing-key";

	private AmqpApi() {
	}

	/**
	 * Global AMQP API not belonging to a specific service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Global {

		private static final String SCOPE = "global";

		public static final ExchangeDefinition<ServiceName> EVENT_FINISHED = ExchangeDefinition.event(SCOPE, "finished").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

		public static final ExchangeDefinition<ServiceName> EVENT_FAILED = ExchangeDefinition.event(SCOPE, "failed").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

		public static final ExchangeDefinition<ServiceNameAndTarget> TASK_CREATE = ExchangeDefinition.task(SCOPE, "create").nonDurable().autoDelete().withRoutingKey(ServiceNameAndTarget.INSTANCE);

		private Global() {
		}

	}

	/**
	 * AMQP API of the orchestrator service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Orchestrator {

		private static final String SCOPE = "orchestrator";

		public static final ExchangeDefinition<RecipeId> EVENT_FINISHED = ExchangeDefinition.event(SCOPE, "finished").nonDurable().nonAutoDelete().withRoutingKey(RecipeId.INSTANCE);

		public static final ExchangeDefinition<ServiceName> EVENT_CONFIG_AVAILABLE = ExchangeDefinition.event(SCOPE, "configavailable").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

		private Orchestrator() {
		}

	}

	/**
	 * AMQP API of the cobra service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Cobra {

		private static final String SCOPE = "cobra";

		public static final ExchangeDefinition<AppIdAndVersion> TASK_PROCESS_TRACES = ExchangeDefinition.task(SCOPE, "process_traces").nonDurable().autoDelete()
				.withRoutingKey(AppIdAndVersion.INSTANCE);

		private Cobra() {
		}

		/**
		 * AMQP API of the clustinator service (sidekick of Cobra).
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Clustinator {

			private static final String SCOPE = "clustinator";

			public static final ExchangeDefinition<AppId> TASK_CLUSTER = ExchangeDefinition.task(SCOPE, "cluster").nonDurable().autoDelete().withRoutingKey(AppId.INSTANCE);

			public static final ExchangeDefinition<AppId> EVENT_FINISHED = ExchangeDefinition.event(SCOPE, "finished").nonDurable().autoDelete().withRoutingKey(AppId.INSTANCE);

			private Clustinator() {
			}

		}

	}

}
