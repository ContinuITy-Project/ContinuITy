/**
 */
package org.continuity.idpa.annotation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Retrieves the input data from a CSV file.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "file", "column", "separator", "header", "columns", "associated" })
public class CsvInput extends ListInput {

	private static final String DEFAULT_SEPARATOR = ";";

	@JsonProperty(value = "file")
	private String filename;

	@JsonProperty(value = "column")
	@JsonInclude(value = Include.CUSTOM, valueFilter = ColumnValueFilter.class)
	private int column = -1;

	@JsonProperty(value = "separator")
	@JsonInclude(value = Include.CUSTOM, valueFilter = ValueFilter.class)
	private String separator = DEFAULT_SEPARATOR;

	@JsonProperty(value = "header")
	private boolean header = false;

	@JsonProperty(value = "columns")
	@JsonInclude(Include.NON_EMPTY)
	private List<CsvColumnInput> columns;

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

	public List<CsvColumnInput> getColumns() {
		return columns;
	}

	public void setColumns(List<CsvColumnInput> columns) {
		this.columns = columns;
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

	public boolean hasHeader() {
		return header;
	}

	public void setHeader(boolean header) {
		this.header = header;
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

	private static class ValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return DEFAULT_SEPARATOR.equals(obj);
		}

	}

	private static final class ColumnValueFilter {

		@Override
		public boolean equals(Object obj) {
			return (obj != null) && (obj instanceof Number) && (((Number) obj).intValue() < 0);
		}

	}

}
