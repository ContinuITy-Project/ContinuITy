package org.continuity.cli.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Henning Schulz
 *
 */
public class PropertiesProvider {

	public static final String DEFAULT_PATH = "continuity.cli.properties";

	public static final String KEY_URL = "continuity.url";

	public static final String KEY_WORKING_DIR = "continuity.working.dir";

	public static final String DEFAULT_WORKING_DIR = "./cli";

	private final Properties properties = new Properties();

	private String path;

	public void init(String path) throws FileNotFoundException, IOException {
		this.path = path;
		if (new File(path).exists()) {
			properties.load(new FileReader(path));
		}

		String workingDir = properties.getProperty(KEY_WORKING_DIR);
		new File(workingDir).mkdirs();
	}

	public boolean isInitialized() {
		return path != null;
	}

	public String getPath() {
		return this.path;
	}

	public void save() throws IOException {
		properties.store(new FileWriter(path), "ContinuITy CLI properties");
	}

	/**
	 * Gets {@link #properties}.
	 *
	 * @return {@link #properties}
	 */
	public Properties get() {
		if (path == null) {
			try {
				init(DEFAULT_PATH);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			putDefaultProperties();
		}

		return this.properties;
	}

	private void putDefaultProperties() {
		properties.putIfAbsent(KEY_WORKING_DIR, DEFAULT_WORKING_DIR);
	}

}
