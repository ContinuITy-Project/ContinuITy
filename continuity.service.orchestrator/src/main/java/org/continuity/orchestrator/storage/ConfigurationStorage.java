package org.continuity.orchestrator.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.continuity.api.entities.config.ServiceConfiguration;
import org.continuity.api.entities.config.session.logs.SessionLogsConfiguration;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Storage for {@link ServiceConfiguration}s.
 *
 * @author Henning Schulz
 *
 */
public class ConfigurationStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStorage.class);

	private static final String FILE_EXT = ".yml";

	private final Path path;

	private final ObjectMapper mapper;

	public ConfigurationStorage(Path path, ObjectMapper mapper) {
		this.path = path;
		this.mapper = mapper;
	}

	/**
	 * Stores a new configuration and potentially overwrites an existing one.
	 *
	 * @param config
	 *            The configuration to be stored.
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public void put(ServiceConfiguration config) throws JsonGenerationException, JsonMappingException, IOException {
		mapper.writerWithDefaultPrettyPrinter().writeValue(toFile(config), config);
	}

	/**
	 * Reads and returns the configuration stored for a given app-id and service.
	 *
	 * @param service
	 *            The ContinuITy service.
	 * @param aid
	 *            The app-id.
	 * @return The configuration or {@code null} if there is none.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public ServiceConfiguration get(String service, AppId aid) throws JsonParseException, JsonMappingException, IOException {
		File file = toFile(service, aid, false);

		if (file.exists()) {
			return mapper.readValue(file, ServiceConfiguration.class);
		} else {
			return null;
		}
	}

	/**
	 * Reads and returns all configurations stored for a given service.
	 *
	 * @param service
	 *            The ContinuITy service.
	 * @return The list of configurations. May be empty.
	 * @throws IOException
	 *             If existing files cannot be listed.
	 */
	public List<ServiceConfiguration> get(String service) throws IOException {
		Path dir = getDir(service, false);

		if (dir.toFile().exists()) {
			return Files.list(dir).map(this::readCatched).filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Reads and returns all existing configurations.
	 *
	 * @return The list of configurations. May be empty.
	 * @throws IOException
	 *             If existing files cannot be listed.
	 */
	public List<ServiceConfiguration> getAll() throws IOException {
		return Files.list(path).map(Path::getFileName).map(Path::toString).filter(name -> !name.startsWith(".")).map(this::getCatched).filter(Objects::nonNull).flatMap(List::stream)
				.collect(Collectors.toList());
	}

	/**
	 * Generates a default configuration.
	 *
	 * @param service The service for which the configuration is to be generated.
	 * @param aid The app-id.
	 * @return The default configuration.
	 */
	public ServiceConfiguration getDefault(String service, AppId aid) {
		ServiceConfiguration config;

		switch (service) {
		case "session-logs":
			config = new SessionLogsConfiguration();
			break;
		default:
			return null;
		}

		config.init(aid);
		return config;
	}

	private List<ServiceConfiguration> getCatched(String service) {
		try {
			return get(service);
		} catch (IOException e) {
			LOGGER.error("Cannot get configurations for " + service, e);
			return null;
		}
	}

	private ServiceConfiguration readCatched(Path file) {
		if (file.getFileName().toString().startsWith(".")) {
			return null;
		}

		try {
			return mapper.readValue(file.toFile(), ServiceConfiguration.class);
		} catch (IOException e) {
			LOGGER.error("Could not read configuration file " + file + "! Ignoring it.", e);
			return null;
		}
	}

	private File toFile(ServiceConfiguration config) {
		return toFile(config.getService(), config.getAppId(), true);
	}

	private File toFile(String service, AppId aid, boolean create) {
		return getDir(service, create).resolve(aid + FILE_EXT).toFile();
	}

	private Path getDir(String service, boolean create) {
		Path dir = path.resolve(service);

		if (create) {
			dir.toFile().mkdirs();
		}

		return dir;
	}

}
