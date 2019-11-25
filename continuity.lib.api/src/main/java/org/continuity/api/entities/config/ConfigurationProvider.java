package org.continuity.api.entities.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.exception.ServiceConfigurationException;
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

	private final Map<AppId, T> configurations = new ConcurrentHashMap<>();

	private final List<Pair<Listener<T>, String>> listeners = new ArrayList<>();

	private final Constructor<T> constructor;

	private boolean initialized = false;

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
	 * @throws ServiceConfigurationException
	 */
	public void init(List<T> configurations) throws ServiceConfigurationException {
		LOGGER.info("Initializing the configurations...");

		ServiceConfigurationException exception = null;

		for (T config : configurations) {
			try {
				refresh(config);
			} catch (ServiceConfigurationException e) {
				LOGGER.error("Error when refreshing config " + config.getAppId(), e);
				exception = e;
			}
		}

		LOGGER.info("Initialization done.");

		synchronized (this) {
			initialized = true;
			this.notifyAll();
		}

		if (exception != null) {
			throw exception;
		}
	}

	/**
	 * Waits until the configuration has been initialized (regardless whether the initialization has
	 * thrown an error).
	 *
	 * @param timeout
	 *            The time to wait at most.
	 */
	public void waitForInitialization(long timeout) {
		LOGGER.info("Waiting for {} sec for the configuration to be initialized...", timeout / 1000);

		while (!initialized) {
			synchronized (this) {
				try {
					this.wait(timeout);
				} catch (InterruptedException e) {
					LOGGER.error("Waiting for initialization interrupted!", e);
				}
			}
		}
	}

	/**
	 * Waits until the configuration has been initialized (regardless whether the initialization has
	 * thrown an error). Will wait at most 5 min.
	 */
	public void waitForInitialization() {
		waitForInitialization(5 * 60 * 1000);
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
	 * Gets the stored configuration or the default one if no such exists. Make sure to call
	 * {@link #refresh(ServiceConfiguration)} if you changed the configuration. Otherwise, the
	 * changes might not be propagated or even result in inconsistent configurations.
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
				T existing = configurations.putIfAbsent(aid, config);
				if (existing == null) {
					config.init(aid);
				} else {
					config = existing;
				}
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
	 * @throws ServiceConfigurationException
	 */
	public void refresh(T config) throws ServiceConfigurationException {
		storeConfig(config);

		for (Pair<Listener<T>, String> pair : listeners) {
			pair.getLeft().accept(config);
		}
	}

	/**
	 * Updates the stored configuration and notifies all listeners except for the ignored ones.
	 *
	 * @param config
	 * @param ignoredListeners
	 *            Listeners not to be notified.
	 * @throws ServiceConfigurationException
	 */
	public void refresh(T config, String... ignoredListeners) throws ServiceConfigurationException {
		storeConfig(config);

		Set<String> ignored = new HashSet<>(Arrays.asList(ignoredListeners));

		for (Pair<Listener<T>, String> pair : listeners) {
			if (!ignored.contains(pair.getRight())) {
				pair.getLeft().accept(config);
			}
		}
	}

	private void storeConfig(T config) {
		this.configurations.put(config.getAppId(), config);

		LOGGER.info("Configuration for app-id {} refreshed.", config.getAppId());
	}

	/**
	 * Registers a listener that will be called when the configuration changes.
	 *
	 * @param listener
	 */
	public void registerListener(Listener<T> listener, String id) {
		listeners.add(Pair.of(listener, id));
	}

	public static interface Listener<T> {

		void accept(T config) throws ServiceConfigurationException;

	}

}
