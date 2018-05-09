package org.continuity.api.amqp;

import org.continuity.api.amqp.RoutingKeyFormatter.Keyword;
import org.continuity.api.amqp.RoutingKeyFormatter.LoadTestType;
import org.continuity.api.amqp.RoutingKeyFormatter.ServiceName;
import org.continuity.api.amqp.RoutingKeyFormatter.Tag;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadAndLoadTestType;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadType;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadTypeAndLink;

/**
 * Holds all AMQP exchange definitions of all ContinuITy services.
 *
 * @author Henning Schulz
 *
 */
public class AmqpApi {

	public static final ExchangeDefinition<ServiceName> DEAD_LETTER_EXCHANGE = ExchangeDefinition.of("global", "dead", "letter").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

	public static final String DEAD_LETTER_EXCHANGE_KEY = "x-dead-letter-exchange";

	public static final String DEAD_LETTER_ROUTING_KEY_KEY = "x-dead-letter-routing-key";

	private AmqpApi() {
	}

	/**
	 * AMQP API of the frontend.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Frontend {

		private static final String SCOPE = "frontend";

		public static final ExchangeDefinition<WorkloadType> DATA_AVAILABLE = ExchangeDefinition.of(SCOPE, "data", "available").nonDurable().autoDelete().withRoutingKey(WorkloadType.INSTANCE);

		public static final ExchangeDefinition<LoadTestType> LOADTESTEXECUTION_REQUIRED = ExchangeDefinition.of(SCOPE, "loadtestexecution", "required").nonDurable().autoDelete()
				.withRoutingKey(LoadTestType.INSTANCE);

		public static final ExchangeDefinition<WorkloadAndLoadTestType> LOADTESTCREATIONANDEXECUTION_REQUIRED = ExchangeDefinition.of(SCOPE, "loadtestcreationandexecution", "required").nonDurable()
				.autoDelete().withRoutingKey(WorkloadAndLoadTestType.INSTANCE);

		private Frontend() {
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

		public static final ExchangeDefinition<Keyword> MESSAGE_AVAILABLE = ExchangeDefinition.of(SCOPE, "message", "available").nonDurable().autoDelete().withRoutingKey(Keyword.INSTANCE);

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

		public static final ExchangeDefinition<Tag> APPLICATION_CHANGED = ExchangeDefinition.of(SCOPE, "application", "changed").nonDurable().autoDelete().withRoutingKey(Tag.INSTANCE);

		private IdpaApplication() {
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

		public static final ExchangeDefinition<LoadTestType> REPORT_AVAILABLE = ExchangeDefinition.of(SCOPE, "report", "available").nonDurable().autoDelete().withRoutingKey(LoadTestType.INSTANCE);

		private LoadTest() {
		}

	}

	/**
	 * AMQP API of the workload services, e.g., wessbas.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Workload {

		private static final String SCOPE = "workload";

		// Not declaring auto delete, since queues are bound dynamically so that the exchange might
		// have no queue for a while
		public static final ExchangeDefinition<WorkloadTypeAndLink> MODEL_CREATED = ExchangeDefinition.of(SCOPE, "model", "created").nonDurable().nonAutoDelete()
				.withRoutingKey(WorkloadTypeAndLink.INSTANCE);

		private Workload() {
		}

	}

}
