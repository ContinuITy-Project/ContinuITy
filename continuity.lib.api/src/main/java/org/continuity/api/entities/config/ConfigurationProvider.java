package org.continuity.api.entities.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds and manages a {@link ServiceConfiguration}.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            Specific type of configuration.
 */
public class ConfigurationProvider<T extends ServiceConfiguration> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationProvider.class);

	private final Map<AppId, T> configurations = new HashMap<>();

	private final List<Consumer<T>> listeners = new ArrayList<>();

	private final Constructor<T> constructor;

	public ConfigurationProvider(Class<T> type) {
		Constructor<T> constructor = null;

		if (type == null) {
			LOGGER.warn("No type specified! Hence, no default instances can be created.");
		} else {
			try {
				constructor = type.getConstructor();
			} catch (NoSuchMethodException | SecurityException e) {
				LOGGER.warn("Could not get the default constructor! Hence, no default instances can be created.", e);
			}
		}

		this.constructor = constructor;
	}

	/**
	 * Initializes the provider with a list of configurations.
	 *
	 * @param configurations
	 */
	public void init(List<T> configurations) {
		LOGGER.info("Initializing the configurations...");
		configurations.forEach(this::refresh);
		LOGGER.info("Initialization done.");
	}

	/**
	 * Gets the stored configuration or {@code null} if no one is specified.
	 *
	 * @param aid
	 * @return
	 */
	public T getConfigurationOrNull(AppId aid) {
		return configurations.get(aid);
	}

	/**
	 * Gets the stored configuration or the default one if no such exists.
	 *
	 * @param aid
	 * @return
	 */
	public T getConfiguration(AppId aid) {
		T config = getConfigurationOrNull(aid);

		if ((config == null) && !aid.isAll()) {
			config = getConfigurationOrNull(aid.dropService());
		}

		if (config == null) {
			try {
				config = constructor.newInstance();
				config.init(aid);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				LOGGER.error("Could not instantiate new ServiceConfiguration!", e);
				return null;
			}
		}

		return config;
	}

	/**
	 * Updates the stored configuration and notifies all listeners.
	 *
	 * @param config
	 */
	public void refresh(T config) {
		this.configurations.put(config.getAppId(), config);

		LOGGER.info("Configuration for app-id {} refreshed.", config.getAppId());

		listeners.forEach(c -> c.accept(config));
	}

	/**
	 * Registers a listener that will be called when the configuration changes.
	 *
	 * @param listener
	 */
	public void registerListener(Consumer<T> listener) {
		listeners.add(listener);
	}

}
