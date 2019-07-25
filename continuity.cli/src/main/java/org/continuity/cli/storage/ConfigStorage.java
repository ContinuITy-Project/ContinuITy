package org.continuity.cli.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.idpa.AppId;

public class ConfigStorage {

	private final DirectoryManager directory;

	public ConfigStorage(PropertiesProvider properties) {
		this.directory = new DirectoryManager("config", properties);
	}

	public Path store(String config, String service, AppId aid) throws IOException {
		return Files.write(getPath(service, aid, true), config.getBytes());
	}

	public String read(String service, AppId aid) throws IOException {
		Path path = getPath(service, aid, false);

		if (path.toFile().exists()) {
			return new String(Files.readAllBytes(path));
		} else {
			return null;
		}
	}

	private Path getPath(String service, AppId aid, boolean create) {
		return directory.getDir(aid, create).resolve(service + ".yml");
	}

}
