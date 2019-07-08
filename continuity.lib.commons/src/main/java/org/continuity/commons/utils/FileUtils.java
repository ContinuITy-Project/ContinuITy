package org.continuity.commons.utils;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Utils for file operations.
 *
 * @author Henning Schulz
 *
 */
public class FileUtils {

	private FileUtils() {
	}

	/**
	 * Collects all files that match a certain path including wildcards.
	 *
	 * @param wildcards
	 *            The path including wildcards.
	 * @return A collection of all matching files.
	 */
	public static Collection<File> getAllFilesMatchingWildcards(String wildcards) {
		File searchDir;
		int indexBeforeFile = wildcards.lastIndexOf(FileSystems.getDefault().getSeparator());

		String filename;

		if (indexBeforeFile < 0) {
			searchDir = new File("./");
			filename = wildcards;
		} else {
			searchDir = new File(wildcards.substring(0, indexBeforeFile));
			filename = wildcards.substring(indexBeforeFile + 1);
		}

		return org.apache.commons.io.FileUtils.listFiles(searchDir, new WildcardFileFilter(filename), new AndFileFilter(DirectoryFileFilter.DIRECTORY, new RegexFileFilter(searchDir.getName())));
	}

	/**
	 * Collects all files that match a certain path including wildcards.
	 *
	 * @param path
	 *            The path including wildcards.
	 * @return A collection of all matching files.
	 */
	public static Collection<File> getAllFilesMatchingWildcards(Path path) {
		return getAllFilesMatchingWildcards(path.toString());
	}

}
