package org.continuity.api.amqp;

import org.springframework.amqp.core.TopicExchange;

/**
 * Defines one exchange.
 *
 * @author Henning Schulz
 *
 * @param <F>
 *            The {@link RoutingKeyFormatter} type.
 */
public class ExchangeDefinition<F extends RoutingKeyFormatter> {

	private static final String CONTINUITY_PREFIX = "continuity.";

	private final String name;

	private final boolean durable;

	private final boolean autoDelete;

	private final F routingKeyFormatter;

	protected ExchangeDefinition(String name, boolean durable, boolean autoDelete, F routingKeyFormatter) {
		this.name = name;
		this.durable = durable;
		this.autoDelete = autoDelete;
		this.routingKeyFormatter = routingKeyFormatter;
	}

	protected static ExchangeBuilder event(String scope, String event) {
		return new ExchangeBuilder("event." + scope + "." + event);
	}

	protected static ExchangeBuilder task(String scope, String task) {
		return new ExchangeBuilder("task." + scope + "." + task);
	}

	/**
	 * Gets the name of this exchange.
	 *
	 * @return The name as string.
	 */
	public String name() {
		return CONTINUITY_PREFIX + name;
	}

	/**
	 * Creates an instance of this exchange.
	 *
	 * @return An {@link TopicExchange}.
	 */
	public TopicExchange create() {
		return new TopicExchange(name(), durable, autoDelete);
	}

	/**
	 * Formats the appropriate routing key.
	 *
	 * @return A formatter that allows to format the key.
	 */
	public F formatRoutingKey() {
		return routingKeyFormatter;
	}

	/**
	 * Derives a queue name for a listening service from the exchange definition.
	 *
	 * @param listeningService
	 *            The name of the listening service.
	 * @return A queue name.
	 */
	public String deriveQueueName(String listeningService) {
		return CONTINUITY_PREFIX + listeningService + "." + name;
	}

	@Override
	public String toString() {
		return name();
	}

}
