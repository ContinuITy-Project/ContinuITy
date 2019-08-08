package org.continuity.commons.accesslogs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.continuity.commons.idpa.UrlPartParameterExtractor;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;

/**
 *
 * @author Henning Schulz
 *
 */
public class UnifiedCsvFromAccessLogsExtractor extends AbstractAccessLogsConsumer {

	private final Path outputPath;

	private BufferedWriter writer;

	public UnifiedCsvFromAccessLogsExtractor(Application application, Path pathToAccessLogs, Path outputPath) {
		super(application, pathToAccessLogs);

		this.outputPath = outputPath;
	}

	@Override
	protected void init() throws IOException {
		Path outputDir = outputPath.getParent();

		if (outputDir != null) {
			outputDir.toFile().mkdirs();
		}

		writer = Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE);
		writer.write(AccessLogEntry.CSV_HEADER);
		writer.newLine();
	}

	@Override
	protected void process(AccessLogEntry logEntry, HttpEndpoint endpoint) throws IOException {
		UrlPartParameterExtractor extractor = new UrlPartParameterExtractor(endpoint, logEntry.getPath());

		List<ParameterRecord> params = new ArrayList<>();

		while (extractor.hasNext()) {
			String name = extractor.nextParameter();
			String value = extractor.currentValue();
			params.add(new ParameterRecord(name, value));
		}

		logEntry.setUrlParameters(params);
		logEntry.setPath(endpoint.getPath());
		logEntry.setEndpoint(endpoint.getId());

		writer.write(logEntry.toCsvRow());
		writer.newLine();
	}

	@Override
	protected void finalize() throws IOException {
		writer.close();
	}

}
