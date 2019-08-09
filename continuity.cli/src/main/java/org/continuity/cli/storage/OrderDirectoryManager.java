package org.continuity.cli.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.idpa.AppId;

/**
 * Manages the CLI working directory for a given context based on order IDs.
 *
 * @author Henning Schulz
 *
 */
public class OrderDirectoryManager extends DirectoryManager {

	private static final String FOLDER_ARCHIVE = "archive";

	public OrderDirectoryManager(String context, PropertiesProvider properties) {
		super(context, properties);
	}

	/**
	 * Gets the path to a folder for a given app-id and order-id.
	 *
	 * @param aid
	 *            The app-id.
	 * @param orderId
	 *            The order-id
	 * @param create
	 *            Whether the folder should be created.
	 * @return The path to the directory. Depending on {@code create}, the folder might not exist.
	 */
	public Path getDir(AppId aid, String orderId, boolean create) {
		Path dir = getDir(aid, false).resolve(orderId);

		if (create && !dir.toFile().exists()) {
			dir.toFile().mkdirs();
		}

		return dir;
	}

	/**
	 * Gets and creates a fresh path to a folder for a given app-id and order-id. If such a folder
	 * already exists, all existing folders will be archived.
	 *
	 * @param aid
	 *            The app-id.
	 * @param orderId
	 *            The order-id
	 * @return The path to the directoy. It is guaranteed that it will exist.
	 */
	public Path getFreshDir(AppId aid, String orderId) {
		Path dir = getDir(aid, false).resolve(orderId);

		if (dir.toFile().exists()) {
			archiveExisting(aid);
		}

		dir.toFile().mkdirs();

		return dir;
	}

	/**
	 * Gets that path to the latest order-id for a given app-id.
	 *
	 * @param aid
	 *            The app-id.
	 * @return The path to the directory or {@code null} if there are no orders.
	 * @throws IOException
	 */
	public Path getLatest(AppId aid) throws IOException {
		Path dir = getDir(aid, false);

		if (!dir.toFile().exists()) {
			return null;
		}

		Optional<String> latest = Files.list(dir).filter(p -> p.toFile().isDirectory()).map(Path::getFileName).map(Path::toString).filter(name -> !name.startsWith(FOLDER_ARCHIVE))
				.sorted(this::compareFolders)
				.reduce((a, b) -> b);

		return latest.map(s -> dir.resolve(s)).orElse(null);
	}

	private int compareFolders(String first, String second) {
		Pattern pattern = Pattern.compile("(.*)-([0-9]+)");
		Matcher firstMatcher = pattern.matcher(first);
		Matcher secondMatcher = pattern.matcher(second);

		if (firstMatcher.find() && secondMatcher.find()) {
			int stringComp = firstMatcher.group(1).compareTo(secondMatcher.group(1));
			int firstNum = Integer.parseInt(firstMatcher.group(2));
			int secondNum = Integer.parseInt(secondMatcher.group(2));

			return (Integer.signum(stringComp) * 2) + Integer.signum(firstNum - secondNum);
		} else {
			return first.compareTo(second);
		}
	}

	public int clearArchive(AppId aid) {
		Path dir = getDir(aid, false).resolve(FOLDER_ARCHIVE);

		if (!dir.toFile().exists()) {
			return 0;
		}

		int deleted = listFiles(dir).flatMap(this::listFiles).map(Path::toFile).mapToInt(this::deleteAndCount).sum();

		dir.toFile().delete();

		return deleted;
	}

	public int clearCurrent(AppId aid) {
		Path dir = getDir(aid, false);

		if (!dir.toFile().exists()) {
			return 0;
		}

		return listFiles(dir).map(Path::toFile).filter(File::isDirectory).mapToInt(this::deleteAndCount).sum();
	}

	private int deleteAndCount(File dir) {
		try {
			FileUtils.deleteDirectory(dir);
		} catch (IOException e) {
			return 0;
		}
		return 1;
	}

	private Stream<Path> listFiles(Path dir) {
		try {
			return Files.list(dir);
		} catch (IOException e) {
			return Stream.empty();
		}
	}

	private void archiveExisting(AppId aid) {
		Path archiveDir = getDir(aid, false).resolve(FOLDER_ARCHIVE);
		String date = LocalDate.now().toString();
		String dirName = date;
		int num = 2;

		while (archiveDir.resolve(dirName).toFile().exists()) {
			dirName = date + num++;
		}

		Path archive = archiveDir.resolve(dirName);

		for (File dir : getDir(aid, false).toFile().listFiles(file -> file.isDirectory() && !FOLDER_ARCHIVE.equals(file.getName()))) {
			try {
				FileUtils.moveDirectory(dir, archive.resolve(dir.getName()).toFile());
			} catch (IOException e) {
				System.err.println("Could not move order directory " + dir);
			}
		}
	}

}
