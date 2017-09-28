/**
 */
package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves the input data from a CSV file. Several associated columns of one file can be specified
 * by creating two {@link CsvInput} and adding them to the {@link CsvInput#getAssociated()} of each
 * other.
 *
 * @author Henning Schulz
 *
 */
public class CsvInput implements Input {

	private static final String DEFAULT_SEPARATOR = ";";

	private String name;

	private List<CsvInput> associated;

	private String filename;

	private int column;

	private String separator = DEFAULT_SEPARATOR;


	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the associated inputs.
	 *
	 * @return The associated inputs.
	 */
	public List<CsvInput> getAssociated() {
		if (associated == null) {
			associated = new ArrayList<>();
		}

		return this.associated;
	}

	/**
	 * Sets the associated inputs.
	 *
	 * @param associated
	 *            The associated inputs.
	 */
	public void setAssociated(List<CsvInput> associated) {
		this.associated = associated;
	}

	/**
	 * Returns the filename of the CSV file.
	 *
	 * @return The filename.
	 */
	public String getFilename() {
		return this.filename;
	}

	/**
	 * Sets the filename of the CSV file.
	 *
	 * @param name
	 *            The filename.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Returns the column of the CSV file.
	 *
	 * @return The column.
	 */
	public int getColumn() {
		return this.column;
	}

	/**
	 * Sets the column of the CSV file.
	 *
	 * @param column
	 *            The column.
	 */
	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * Returns the separator of the CSV file.
	 *
	 * @return The separator.
	 */
	public String getSeparator() {
		return this.separator;
	}

	/**
	 * Sets the separator of the CSV file.
	 *
	 * @param separator
	 *            The separator.
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(", filename: ");
		result.append(filename);
		result.append(", column: ");
		result.append(column);
		result.append(", separator: ");
		result.append(separator);
		result.append(')');
		return result.toString();
	}
}
