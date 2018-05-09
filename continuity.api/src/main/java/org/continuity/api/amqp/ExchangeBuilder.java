package org.continuity.api.amqp;

/**
 * Builds an exchange description.
 *
 * @author Henning Schulz
 *
 * @see ExchangeDefinition
 *
 */
public class ExchangeBuilder {

	private final String name;

	private boolean durable;

	private boolean autoDelete;

	/**
	 * Initializes the builder with the exchange name.
	 *
	 * @param name
	 *            The name of the exchange.
	 */
	public ExchangeBuilder(String name) {
		this.name = name;
	}

	/**
	 * Defines the exchange to be durable.
	 *
	 * @return The builder for further specifications.
	 */
	public ExchangeBuilder durable() {
		this.durable = true;
		return this;
	}

	/**
	 * Defines the exchange not to be durable.
	 *
	 * @return The builder for further specifications.
	 */
	public ExchangeBuilder nonDurable() {
		this.durable = false;
		return this;
	}

	/**
	 * Defines the exchange to be automatically deleted.
	 *
	 * @return The builder for further specifications.
	 */
	public ExchangeBuilder autoDelete() {
		this.autoDelete = true;
		return this;
	}

	/**
	 * Defines the exchange not to be automatically deleted.
	 *
	 * @return The builder for further specifications.
	 */
	public ExchangeBuilder nonAutoDelete() {
		this.autoDelete = false;
		return this;
	}

	/**
	 * Creates the exchange definition with a routing key formatter.
	 *
	 * @param formatter
	 *            The routing key formatter.
	 * @return The created exchange definition.
	 */
	public <F extends RoutingKeyFormatter> ExchangeDefinition<F> withRoutingKey(F formatter) {
		return new ExchangeDefinition<>(name, durable, autoDelete, formatter);
	}

}
