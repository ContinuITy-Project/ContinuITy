package org.continuity.api.amqp;

import org.continuity.api.amqp.RoutingKeyFormatter.Keyword;
import org.continuity.api.amqp.RoutingKeyFormatter.LoadTestType;
import org.continuity.api.amqp.RoutingKeyFormatter.RecipeId;
import org.continuity.api.amqp.RoutingKeyFormatter.ServiceName;
import org.continuity.api.amqp.RoutingKeyFormatter.Tag;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadType;

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

		private Orchestrator() {
		}

	}

	/**
	 * AMQP API of the session logs service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class SessionLogs {

		private static final String SCOPE = "sessionlogs";

		public static final ExchangeDefinition<Tag> TASK_CREATE = ExchangeDefinition.task(SCOPE, "create").nonDurable().autoDelete().withRoutingKey(Tag.INSTANCE);

		private SessionLogs() {
		}

	}

	/**
	 * AMQP API of the workload model services, e.g., wessbas.
	 *
	 * @author Henning Schulz, Alper Hidiroglu
	 *
	 */
	public static class WorkloadModel {

		private static final String SCOPE = "workloadmodel";

		public static final ExchangeDefinition<WorkloadType> TASK_CREATE = ExchangeDefinition.task(SCOPE, "create").nonDurable().autoDelete().withRoutingKey(WorkloadType.INSTANCE);
		
		public static final ExchangeDefinition<WorkloadType> MIX_CREATE = ExchangeDefinition.task(SCOPE, "createmix").nonDurable().autoDelete().withRoutingKey(WorkloadType.INSTANCE);

		public static final ExchangeDefinition<WorkloadType> EVENT_CREATED = ExchangeDefinition.event(SCOPE, "created").nonDurable().autoDelete().withRoutingKey(WorkloadType.INSTANCE);

		private WorkloadModel() {
		}

	}

	/**
	 * AMQP API of the load test services, e.g., jmeter.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class LoadTest {

		private static final String SCOPE = "loadtest";

		public static final ExchangeDefinition<LoadTestType> TASK_CREATE = ExchangeDefinition.task(SCOPE, "create").nonDurable().autoDelete().withRoutingKey(LoadTestType.INSTANCE);

		public static final ExchangeDefinition<LoadTestType> TASK_EXECUTE = ExchangeDefinition.task(SCOPE, "execute").nonDurable().autoDelete().withRoutingKey(LoadTestType.INSTANCE);

		private LoadTest() {
		}

	}

	/**
	 * AMQP API of the IDPA annotation service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaAnnotation {

		private static final String SCOPE = "idpaannotation";

		public static final ExchangeDefinition<Keyword> EVENT_MESSAGE = ExchangeDefinition.event(SCOPE, "message").nonDurable().autoDelete().withRoutingKey(Keyword.INSTANCE);

		private IdpaAnnotation() {
		}

	}

	/**
	 * AMQP API of the IDPA application service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaApplication {

		private static final String SCOPE = "idpaapplication";

		public static final ExchangeDefinition<Tag> EVENT_CHANGED = ExchangeDefinition.event(SCOPE, "changed").nonDurable().autoDelete().withRoutingKey(Tag.INSTANCE);

		private IdpaApplication() {
		}

	}
	
	/**
	 * AMQP API of the forecast service.
	 *
	 * @author Alper Hidiroglu
	 *
	 */
	public static class Forecast {

		private static final String SCOPE = "forecast";

		public static final ExchangeDefinition<ServiceName> TASK_CREATE = ExchangeDefinition.task(SCOPE, "create").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);
		
		private Forecast() {
		}

	}

}
