package org.continuity.cli.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.idpa.AppId;

/**
 * Manages the working directory of the CLI for a given context.
 *
 * @author Henning Schulz
 *
 */
public class DirectoryManager {

	private final String context;

	private final PropertiesProvider properties;

	public DirectoryManager(String context, PropertiesProvider properties) {
		this.context = context;
		this.properties = properties;
	}

	/**
	 * Gets the path to a folder for a given app-id.
	 *
	 * @param aid
	 *            The app-id.
	 * @param create
	 *            Whether the folder should be created.
	 * @return The path to the directory. Depending on {@code create}, the folder might not exist.
	 */
	public Path getDir(AppId aid, boolean create) {
		Path dir = Paths.get(properties.getProperty(PropertiesProvider.KEY_WORKING_DIR)).resolve(aid.toString()).resolve(context);

		if (create && !dir.toFile().exists()) {
			dir.toFile().mkdirs();
		}

		return dir;
	}

}
