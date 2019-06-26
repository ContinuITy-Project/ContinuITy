package org.continuity.request.rates.entities;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.univocity.parsers.annotations.Headers;
import com.univocity.parsers.annotations.Parsed;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * A CSV record representing a single request.
 *
 * @author Henning Schulz
 *
 */
@Headers(write = true)
public class CsvRow {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvRow.class);

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss-SSSX");

	private static final String DEFAULT_ENCODING = "<no-encoding>";

	@Parsed
	private String startDate;

	@Parsed
	private String endDate;

	@Parsed
	private String name;

	@Parsed
	private String domain;

	@Parsed
	private String port;

	@Parsed
	private String path;

	@Parsed
	private String method;

	@Parsed(defaultNullRead = DEFAULT_ENCODING)
	private String encoding;

	@Parsed
	private String protocol;

	@Parsed
	private String parameters;

	@Parsed
	private String headers;

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getHeaders() {
		return headers;
	}

	public void setHeaders(String headers) {
		this.headers = headers;
	}

	public boolean checkDates() {
		try {
			if (startDate != null) {
				DATE_FORMAT.parse(startDate);
			}

			if (endDate != null) {
				DATE_FORMAT.parse(endDate);
			}
		} catch (ParseException e) {
			LOGGER.error("Cannot parse date!", e);
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(startDate);
		builder.append(",");
		builder.append(endDate);
		builder.append(",");
		builder.append(name);
		builder.append(",");
		builder.append(domain);
		builder.append(",");
		builder.append(port);
		builder.append(",");
		builder.append(path);
		builder.append(",");
		builder.append(method);
		builder.append(",");
		builder.append(encoding);
		builder.append(",");
		builder.append(protocol);
		builder.append(",");
		builder.append(parameters);
		builder.append(",");
		builder.append(headers);

		return builder.toString();
	}

	public RequestRecord toRecord() {
		RequestRecord record = new RequestRecord();

		if (startDate != null) {
			try {
				record.setStartDate(DATE_FORMAT.parse(startDate));
			} catch (ParseException e) {
				LOGGER.error("Cannot parse start date!", e);
			}
		}

		if (endDate != null) {
			try {
				record.setEndDate(DATE_FORMAT.parse(endDate));
			} catch (ParseException e) {
				LOGGER.error("Cannot parse end date!", e);
			}
		}

		record.setName(name);
		record.setDomain(domain);
		record.setPort(port);
		record.setPath(path);
		record.setMethod(method);
		record.setEncoding(encoding);
		record.setProtocol(protocol);

		if (parameters != null) {
			record.setParameters(Arrays.asList(parameters.split("\\&")));
		}

		if (headers != null) {
			record.setHeaders(Arrays.asList(headers.split("\\&")));
		}

		return record;
	}

	public static List<CsvRow> listFromString(String requestLogs) {
		BeanListProcessor<CsvRow> rowProcessor = new BeanListProcessor<>(CsvRow.class);

		CsvParserSettings settings = new CsvParserSettings();
		settings.setProcessor(rowProcessor);
		settings.setHeaderExtractionEnabled(true);
		settings.setDelimiterDetectionEnabled(true, ',', ';');

		CsvParser parser = new CsvParser(settings);
		parser.parse(new ByteArrayInputStream(requestLogs.getBytes()));

		return rowProcessor.getBeans();
	}

}
