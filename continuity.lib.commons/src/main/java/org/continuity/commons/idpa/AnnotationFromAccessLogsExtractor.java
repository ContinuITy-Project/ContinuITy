package org.continuity.commons.idpa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.commons.accesslogs.AbstractAccessLogsConsumer;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvColumnInput;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;

/**
 * Extracts an IDPA annotation based on an existing application model and Apache access logs.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationFromAccessLogsExtractor extends AbstractAccessLogsConsumer {

	private static final String PREFIX_INPUT = "Input_";

	private static final String SEPARATOR = ",";

	private static final String FILE_EXT_CSV = ".csv";

	private final Application application;

	private final Path outputDir;

	private ApplicationAnnotation extractedAnnotation;

	private Application filteredApplication;

	private Map<HttpEndpoint, ParameterValueCollector> collectorPerEndpoint = new HashMap<>();

	/**
	 *
	 * @param application
	 *            The application model describing the endpoints and parameters.
	 * @param pathToAccessLogs
	 *            The path to the access logs file.
	 * @param outputDir
	 *            The directory where potentially created CSV files should be stored.
	 */
	public AnnotationFromAccessLogsExtractor(Application application, Path pathToAccessLogs, Path outputDir) {
		super(application, pathToAccessLogs);

		this.application = application;
		this.outputDir = outputDir;
	}

	public void extract() throws IOException {
		consume();
	}

	@Override
	protected void init() {
	}

	@Override
	protected void process(AccessLogEntry logEntry, HttpEndpoint endpoint) {
		getCollectorForEndpoint(endpoint).collectFromPath(logEntry.getPath());
		getCollectorForEndpoint(endpoint).collectFromQueryString(logEntry.getRequestParameters());
	}

	@Override
	protected void finalize() throws IOException {
		collectFilteredApplication();
		collectExtractedAnnotation();
	}

	private ParameterValueCollector getCollectorForEndpoint(HttpEndpoint endpoint) {
		ParameterValueCollector values = collectorPerEndpoint.get(endpoint);

		if (values == null) {
			values = new ParameterValueCollector(endpoint);
			collectorPerEndpoint.put(endpoint, values);
		}

		return values;
	}

	private void collectFilteredApplication() {
		filteredApplication = new Application();
		filteredApplication.setId(application.getId());
		filteredApplication.setVersionOrTimestamp(application.getVersionOrTimestamp());
		filteredApplication.getEndpoints().addAll(collectorPerEndpoint.keySet());
	}

	private void collectExtractedAnnotation() throws IOException {
		extractedAnnotation = new ApplicationAnnotation();

		for (ParameterValueCollector collector : collectorPerEndpoint.values()) {
			EndpointAnnotation endpointAnn = createEndpointAnnotation(collector.getEndpoint());
			Map<HttpParameter, List<String>> valuesPerParam = collector.getValuesPerParam();

			if (valuesPerParam.size() == 1) {
				HttpParameter param = valuesPerParam.keySet().iterator().next();
				collectValuesOfParameter(endpointAnn, param, valuesPerParam.get(param));
			} else if (valuesPerParam.size() > 1) {
				collectValuesOfMultipleParameters(endpointAnn, valuesPerParam);
			}
		}
	}

	private void collectValuesOfParameter(EndpointAnnotation endpointAnn, HttpParameter param, List<String> values) {
		DirectListInput input = new DirectListInput();
		input.setId(PREFIX_INPUT + param.getId());

		List<String> distinctValues = values.stream().distinct().collect(Collectors.toList());

		if (distinctValues.size() == 1) {
			input.setData(distinctValues);
		} else {
			input.setData(values);
		}

		extractedAnnotation.addInput(input);

		createParameterAnnotation(endpointAnn, param, input);
	}

	private void collectValuesOfMultipleParameters(EndpointAnnotation endpointAnn, Map<HttpParameter, List<String>> valuesPerParam) throws IOException {
		String csvFile = createCsvFile(endpointAnn.getAnnotatedEndpoint().getId(), valuesPerParam);
		CsvInput input = new CsvInput();
		input.setFilename(csvFile);
		input.setSeparator(SEPARATOR);
		input.setHeader(true);
		input.setColumns(new ArrayList<>(valuesPerParam.size()));
		extractedAnnotation.addInput(input);

		for (HttpParameter param : valuesPerParam.keySet()) {
			CsvColumnInput colInput = new CsvColumnInput();
			colInput.setId(PREFIX_INPUT + param.getId());
			input.getColumns().add(colInput);
			createParameterAnnotation(endpointAnn, param, colInput);
		}
	}

	private EndpointAnnotation createEndpointAnnotation(HttpEndpoint endpoint) {
		EndpointAnnotation ann = new EndpointAnnotation();
		ann.setAnnotatedEndpoint(WeakReference.create(endpoint));
		extractedAnnotation.getEndpointAnnotations().add(ann);
		return ann;
	}

	private ParameterAnnotation createParameterAnnotation(EndpointAnnotation endpointAnn, HttpParameter param, Input input) {
		ParameterAnnotation paramAnn = new ParameterAnnotation();
		paramAnn.setAnnotatedParameter(WeakReference.create(param));
		paramAnn.setInput(input);
		endpointAnn.addParameterAnnotation(paramAnn);
		return paramAnn;
	}

	private String createCsvFile(String endpointId, Map<HttpParameter, List<String>> valuesPerParam) throws IOException {
		int numRows = valuesPerParam.values().iterator().next().size() + 1;
		int numCols = valuesPerParam.size();
		String[][] matrix = new String[numRows][numCols];

		int col = 0;
		for (HttpParameter param : valuesPerParam.keySet()) {
			matrix[0][col] = param.getId();
			col++;
		}

		col = 0;
		for (List<String> valuesInCol : valuesPerParam.values()) {
			int row = 1;

			for (String value : valuesInCol) {
				matrix[row][col] = value;
				row++;
			}

			col++;
		}

		List<String> csvContent = Arrays.stream(matrix).map(Arrays::stream).map(s -> s.collect(Collectors.joining(SEPARATOR))).collect(Collectors.toList());
		String csvFile = endpointId + FILE_EXT_CSV;

		Files.write(outputDir.resolve(csvFile), csvContent, StandardOpenOption.CREATE);

		return csvFile;
	}

	public ApplicationAnnotation getExtractedAnnotation() {
		return this.extractedAnnotation;
	}

	public Application getFilteredApplication() {
		return filteredApplication;
	}

}
