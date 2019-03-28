package org.continuity.commons.idpa;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts an IDPA annotation based on an existing application model and Apache access logs.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationFromAccessLogsExtractor {

	public static final String DEFAULT_ACCESS_LOGS_REGEX = ".* - - \\[[^\\]]+\\] \"([A-Z]+) ([^\"]+) .+\" .*";

	private static final String PREFIX_INPUT = "Input_";

	private static final String SEPARATOR = ",";

	private static final String FILE_EXT_CSV = ".csv";

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationFromAccessLogsExtractor.class);

	private final Application application;

	private final RequestUriMapper mapper;

	private final Path pathToAccessLogs;

	private final Path outputDir;

	private Pattern pattern;

	private ApplicationAnnotation extractedAnnotation;

	private Application filteredApplication;

	private Set<String> ignoredRequests = new LinkedHashSet<>();

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
		this.application = application;
		this.mapper = new RequestUriMapper(application);
		this.pathToAccessLogs = pathToAccessLogs;
		this.outputDir = outputDir;
	}

	/**
	 * Sets the regular expression. If this method is not called,
	 * {@value #DEFAULT_ACCESS_LOGS_REGEX} will be used.
	 *
	 * @param regex
	 *            The regular expression used to extract the request method and path including the
	 *            query. There should be one capture group per property in the mentioned order.
	 */
	public void setRegex(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	public void extract() throws IOException {
		if (pattern == null) {
			setRegex(DEFAULT_ACCESS_LOGS_REGEX);
		}

		BufferedReader reader = Files.newBufferedReader(pathToAccessLogs);

		String line = reader.readLine();

		while (line != null) {
			processLine(line);
			line = reader.readLine();
		}

		collectFilteredApplication();
		collectExtractedAnnotation();
	}

	private void processLine(String line) throws IOException {
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			String method = matcher.group(1);
			String uri = matcher.group(2);
			String[] pathAndQuery = uri.split("\\?");

			HttpEndpoint endpoint = mapper.map(pathAndQuery[0], method);

			if (endpoint == null) {
				ignoredRequests.add(method + " " + uri);
			} else {
				getCollectorForEndpoint(endpoint).collectFromPath(pathAndQuery.length > 0 ? pathAndQuery[0] : uri);
				getCollectorForEndpoint(endpoint).collectFromQueryString(pathAndQuery.length > 1 ? pathAndQuery[1] : null);
			}
		} else {
			LOGGER.error("The regular expression does not match to the log entry: {}", line);
		}
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
		filteredApplication.setTimestamp(application.getTimestamp());
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

	public Set<String> getIgnoredRequests() {
		return ignoredRequests;
	}

}
