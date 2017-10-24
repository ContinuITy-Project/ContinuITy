package org.continuity.wessbas.wessbas2jmeter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * Adapted {@link CSVHandler} that holds the CSV files only in a buffer and writes it to the disk on
 * demand
 *
 * @author Henning Schulz
 *
 */
public class CSVBufferingHandler extends CSVHandler {

	private final Map<String, String[][]> buffer = new HashMap<>();

	/**
	 * <b>NOTE: Does not write to disk, only adds to the buffer.</b><br>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void writeValues(String filePath, String[][] values) throws FileNotFoundException, IOException, SecurityException, NullPointerException {
		buffer.put(filePath, values);
	}

	/**
	 * Actually writes the buffer content to the specified directory.
	 *
	 * @param dir
	 *            Directory where the buffer content should be written.
	 * @throws IOException
	 *             if an error during writing occurs.
	 */
	public void writeToDisk(String dir) throws IOException {
		for (Entry<String, String[][]> entry : buffer.entrySet()) {
			try {
				super.writeValues(dir + "/" + entry.getKey(), entry.getValue());
			} catch (SecurityException | NullPointerException | IOException e) {
				throw new IOException("Error during writing the csv files!", e);
			}
		}

		buffer.clear();
	}

}
