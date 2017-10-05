/**
 */
package org.continuity.workload.dsl.annotation;

/**
 * Retrieves the input data from a CSV file.
 *
 * @author Henning Schulz
 *
 */
public class CsvInput extends DataInput {

	private static final String DEFAULT_SEPARATOR = ";";

	private String filename;

	private int column;

	private String separator = DEFAULT_SEPARATOR;

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
		result.append(" (id: ");
		result.append(getId());
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
