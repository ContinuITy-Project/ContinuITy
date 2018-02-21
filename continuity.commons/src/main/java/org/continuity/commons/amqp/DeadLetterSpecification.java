package org.continuity.commons.amqp;

/**
 * Common constants for RabbitMQ dead letter exchanges.
 *
 * @author Henning Schulz
 *
 */
public class DeadLetterSpecification {

	public static final String EXCHANGE_KEY = "x-dead-letter-exchange";

	public static final String EXCHANGE_NAME = "continuity.dead.letter";

	public static final String ROUTING_KEY_KEY = "x-dead-letter-routing-key";

	private DeadLetterSpecification() {
	}

}
