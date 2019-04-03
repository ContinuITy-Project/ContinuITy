package org.continuity.cli.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.rest.RestApi.Orchestrator.Idpa;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.commons.idpa.AnnotationFromAccessLogsExtractor;
import org.continuity.commons.idpa.AnnotationValidityChecker;
import org.continuity.commons.idpa.ApplicationUpdater;
import org.continuity.commons.idpa.OpenApiToIdpaTransformer;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

/**
 * CLI for annotation handling.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class IdpaCommands {

	private static final String CONTEXT_NAME = "idpa";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("download", this, "downloadIdpa", String.class), //
			new Shorthand("open", this, "openIdpa", String.class), //
			new Shorthand("app", this, "goToIdpaAappContext"), //
			new Shorthand("app upload", this, "uploadApplication", String.class), //
			new Shorthand("app create", this, "createIdpaApplication", String.class, String.class), //
			new Shorthand("app update", this, "updateIdpaApplication", String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class), //
			new Shorthand("app init", this, "initApplication", String.class), //
			new Shorthand("ann", this, "goToIdpaAnnContext"), //
			new Shorthand("ann upload", this, "uploadAnnotation", String.class), //
			new Shorthand("ann init", this, "initAnnotation", String.class), //
			new Shorthand("ann extract", this, "extractAnnotation", String.class, String.class, String.class), //
			new Shorthand("ann check", this, "checkAnnotation", String.class) //
	);

	private static final String APP_CONTEXT_NAME = "app";

	private final CliContext appContext = new CliContext(APP_CONTEXT_NAME, //
			new Shorthand("upload", this, "uploadApplication", String.class), //
			new Shorthand("create", this, "createIdpaApplication", String.class, String.class), //
			new Shorthand("update", this, "updateIdpaApplication", String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class), //
			new Shorthand("open", this, "openIdpa", String.class), //
			new Shorthand("init", this, "initApplication", String.class) //
	);

	private static final String ANN_CONTEXT_NAME = "ann";

	private final CliContext annContext = new CliContext(ANN_CONTEXT_NAME, //
			new Shorthand("upload", this, "uploadAnnotation", String.class), //
			new Shorthand("init", this, "initAnnotation", String.class), //
			new Shorthand("extract", this, "extractAnnotation", String.class, String.class, String.class), //
			new Shorthand("check", this, "checkAnnotation", String.class), //
			new Shorthand("open", this, "openIdpa", String.class) //
	);

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private CliContextManager contextManager;

	private final OpenApiToIdpaTransformer openApiTransfomer = new OpenApiToIdpaTransformer();

	private final IdpaYamlSerializer<Application> appSerializer = new IdpaYamlSerializer<>(Application.class);

	private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

	private final SwaggerParser swaggerParser = new SwaggerParser();

	private final ApplicationUpdater applicationUpdater = new ApplicationUpdater();

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'idpa' context so that the shorthands can be used.")
	public void goToIdpaContext() {
		contextManager.goToContext(context);
	}

	@ShellMethod(key = { "idpa download" }, value = "Downloads and opens the IDPA with the specified tag.")
	public String downloadIdpa(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws JsonGenerationException, JsonMappingException, IOException {
		tag = contextManager.getTagOrFail(tag);

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));

		ResponseEntity<Application> applicationResponse = restTemplate.getForEntity(Idpa.GET_APPLICATION.requestUrl(tag).withHost(url).get(), Application.class);
		ResponseEntity<ApplicationAnnotation> annotationResponse = restTemplate.getForEntity(Idpa.GET_ANNOTATION.requestUrl(tag).withHost(url).get(), ApplicationAnnotation.class);

		if (!applicationResponse.getStatusCode().is2xxSuccessful()) {
			return "Could not get application model: " + applicationResponse;
		}

		if (!annotationResponse.getStatusCode().is2xxSuccessful()) {
			return "Could not get annotation: " + annotationResponse;
		}

		saveApplicationModel(applicationResponse.getBody(), tag);
		saveAnnotation(annotationResponse.getBody(), tag);

		openApplicationModel(tag);
		openAnnotation(tag);

		return "Downloaded and opened the IDPA with tag " + tag;
	}

	@ShellMethod(key = { "idpa open" }, value = "Opens an already downloaded IDPA with the specified tag.")
	public String openIdpa(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws IOException {
		tag = contextManager.getTagOrFail(tag);

		boolean appExists = openApplicationModel(tag);
		boolean annExists = openAnnotation(tag);

		StringBuilder response = new StringBuilder();

		if (appExists) {
			response.append("Opened the IDPA application model with tag ").append(tag);
		} else {
			response.append("The IDPA application model with tag ").append(tag).append(" does not exist.");
		}

		response.append("\n");

		if (annExists) {
			response.append("Opened the IDPA annotation with tag ").append(tag);
		} else {
			response.append("The IDPA annotation with tag ").append(tag).append(" does not exist.");
		}

		return response.toString();
	}

	@ShellMethod(key = { "idpa app" }, value = "Goes to the 'idpa/app' context so that the shorthands can be used.")
	public void goToIdpaAappContext() {
		contextManager.goToContext(context, appContext);
	}

	@ShellMethod(key = { "idpa app init" }, value = "Initializes an application model with the specified tag.")
	public String initApplication(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws JsonParseException, JsonMappingException, IOException {
		tag = contextManager.getTagOrFail(tag);

		Application application = readApplicationModel(tag);

		if (application != null) {
			return "There is already an application model for tag " + tag + "!";
		} else {
			application = new Application();
			application.setId(tag);
			application.setTimestamp(new Date());

			saveApplicationModel(application, tag);
			openApplicationModel(tag);

			return "Initialized and opened the annotation.";
		}
	}

	@ShellMethod(key = { "idpa app create" }, value = "Creates an IDPA application model from an OpenApi.")
	public String createIdpaApplication(String openApiLocation, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag)
			throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {
		tag = contextManager.getTagOrFail(tag);

		Swagger swagger = readSwagger(openApiLocation);
		Application application = openApiTransfomer.transform(swagger);
		application.setId(tag);

		saveApplicationModel(application, tag);
		openApplicationModel(tag);

		return "Created and opened the application model with tag " + tag + " from the OpenAPI at " + openApiLocation;
	}

	@ShellMethod(key = { "idpa app update" }, value = "Updates an IDPA application model from an OpenApi.")
	public String updateIdpaApplication(
			@ShellOption(help = "Location where the OpenAPI model can be found. Can be a URL or a file path. A file name can contain UNIX-like wildcards.") String openApiLocation,
			@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag, @ShellOption(defaultValue = "false", value = { "--add", "-a" }, help = "Consider element additions.") boolean add,
			@ShellOption(defaultValue = "false", value = { "--remove", "-r" }, help = "Consider element removals.") boolean remove,
			@ShellOption(defaultValue = "false", value = { "--change", "-c" }, help = "Consider element changes.") boolean change,
			@ShellOption(defaultValue = "false", value = { "--endpoints", "-e" }, help = "Consider endpoints.") boolean endpoints,
			@ShellOption(defaultValue = "false", value = { "--parameters", "-p" }, help = "Consider parameters.") boolean parameters,
			@ShellOption(defaultValue = "false", value = { "--hide-ignored" }, help = "Consider parameters.") boolean hideIgnored)
			throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {

		StringBuilder response = new StringBuilder();

		tag = contextManager.getTagOrFail(tag);
		Application origApplication = readApplicationModel(tag);
		Application updatedApplication = origApplication;
		EnumSet<ApplicationChangeType> changeTypes = changeTypesFromBooleans(add, remove, change, endpoints, parameters);

		if (!openApiLocation.startsWith("http")) {
			for (File openApiFile : getAllFilesMatchingWildcards(openApiLocation)) {
				ApplicationChangeReport report = updateApplicationModel(updatedApplication, openApiFile.getPath(), tag, changeTypes);
				updatedApplication = report.getUpdatedApplication();
				report.setUpdatedApplication(null);

				if (hideIgnored) {
					report.setIgnoredApplicationChanges(null);
				}

				response.append("Updated with ").append(openApiFile.getPath()).append(":\n").append(mapper.writeValueAsString(report));
			}
		} else {
			ApplicationChangeReport report = updateApplicationModel(origApplication, openApiLocation, tag, changeTypes);
			updatedApplication = report.getUpdatedApplication();
			report.setUpdatedApplication(null);

			if (hideIgnored) {
				report.setIgnoredApplicationChanges(null);
			}

			response.append("Updated with ").append(openApiLocation).append(":\n").append(mapper.writeValueAsString(report));
		}

		String origTag = tag + "_old";
		origApplication.setId(origTag);

		saveApplicationModel(origApplication, origTag);
		saveApplicationModel(updatedApplication, tag);

		openApplicationModel(origTag);
		openApplicationModel(tag);

		return response.toString();
	}

	private ApplicationChangeReport updateApplicationModel(Application origApplication, String openApiLocation, String tag, EnumSet<ApplicationChangeType> changeTypes)
			throws JsonGenerationException, JsonMappingException, IOException {
		Swagger swagger = readSwagger(openApiLocation);

		Application newApplication = openApiTransfomer.transform(swagger);

		ApplicationChangeReport report = applicationUpdater.updateApplication(origApplication, newApplication, changeTypes);

		String origTag = tag + "_old";
		origApplication.setId(origTag);

		saveApplicationModel(origApplication, origTag);
		saveApplicationModel(report.getUpdatedApplication(), tag);

		openApplicationModel(origTag);
		openApplicationModel(tag);

		return report;
	}

	@ShellMethod(key = { "idpa app upload" }, value = "Handle with care! Uploads the application model with the specified tag. Can break the online stored annotation!")
	public String uploadApplication(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "Tag of the application model. Can contain UNIX-like wildcards.") String pattern)
			throws JsonParseException, JsonMappingException, IOException {
		pattern = contextManager.getTagOrFail(pattern);

		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		ResponseEntity<String> response;
		List<String> tags = new ArrayList<String>();

		for (File file : getAllFilesMatchingWildcards(workingDir + "/application-" + pattern + ".yml")) {
			Application application = appSerializer.readFromYaml(file);
			String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
			String tag = file.getName().substring("application-".length(), file.getName().length() - ".yml".length());
			tags.add(tag);
			try {
				response = restTemplate.postForEntity(Idpa.UPDATE_APPLICATION.requestUrl(tag).withHost(url).get(), application, String.class);
			} catch (HttpStatusCodeException e) {
				response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
			}
			if (!response.getStatusCode().is2xxSuccessful()) {
				return "Error during upload: " + response;
			}
		}
		return "Successfully uploaded annotations for tags '" + tags + "'.";

	}

	@ShellMethod(key = { "idpa ann" }, value = "Goes to the 'idpa/ann' context so that the shorthands can be used.")
	public void goToIdpaAnnContext() {
		contextManager.goToContext(context, annContext);
	}

	@ShellMethod(key = { "idpa ann upload" }, value = "Uploads the annotation with the specified tag.")
	public String uploadAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "Tag of the annotation. Can contain UNIX-like wildcards.") String pattern)
			throws JsonParseException, JsonMappingException, IOException {
		pattern = contextManager.getTagOrFail(pattern);

		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		List<String> tags = new ArrayList<String>();

		for (File file : getAllFilesMatchingWildcards(workingDir + "/annotation-" + pattern + ".yml")) {
			ApplicationAnnotation annotation = annSerializer.readFromYaml(file);
			String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
			String tag = file.getName().substring("annotation-".length(), file.getName().length() - ".yml".length());
			tags.add(tag);
			ResponseEntity<String> response;
			try {
				response = restTemplate.postForEntity(Idpa.UPDATE_ANNOTATION.requestUrl(tag).withHost(url).get(), annotation, String.class);
			} catch (HttpStatusCodeException e) {
				response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
			}
			if (!response.getStatusCode().is2xxSuccessful()) {
				return "Error during upload: " + response;
			}
		}
		return "Successfully uploaded annotations for tags '" + tags + "'.";

	}

	private Collection<File> getAllFilesMatchingWildcards(String wildcards) {
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

		return FileUtils.listFiles(searchDir, new WildcardFileFilter(filename), new AndFileFilter(DirectoryFileFilter.DIRECTORY, new RegexFileFilter(searchDir.getName())));
	}

	@ShellMethod(key = { "idpa ann init" }, value = "Initializes an annotation for the stored application model with the specified tag.")
	public String initAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws JsonParseException, JsonMappingException, IOException {
		tag = contextManager.getTagOrFail(tag);

		Application application = readApplicationModel(tag);
		ApplicationAnnotation annotation = new AnnotationExtractor().extractAnnotation(application);

		saveAnnotation(annotation, tag);

		openApplicationModel(tag);
		openAnnotation(tag);

		return "Initialized and opened the annotation.";
	}

	@ShellMethod(key = { "idpa ann extract" }, value = "Extracts an annotation for the stored application model with the specified tag from Apache request logs.")
	public String extractAnnotation(String logsFile, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag,
			@ShellOption(defaultValue = AccessLogEntry.DEFAULT_REGEX, help = "The regular expression used to extract the request method and path including the query. There should be one capture group per property in the mentioned order.") String regex)
			throws IOException {
		tag = contextManager.getTagOrFail(tag);

		Application application = readApplicationModel(tag);
		ApplicationAnnotation annotation = readAnnotation(tag);

		if (application == null) {
			return "There is no application model! Please create one first.";
		}

		if (annotation != null) {
			return "There is already an annotation! Remove or move it first before extracting a new one.";
		}

		Path pathToLogs = Paths.get(logsFile);
		Path workingDir = Paths.get(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR));

		if (pathToLogs.isAbsolute()) {
			pathToLogs = workingDir.resolve(pathToLogs);
		}

		AnnotationFromAccessLogsExtractor extractor = new AnnotationFromAccessLogsExtractor(application, pathToLogs, workingDir);
		extractor.setRegex(regex);
		extractor.extract();

		annotation = extractor.getExtractedAnnotation();
		Application filteredApplication = extractor.getFilteredApplication();
		Set<String> ignoredRequests = extractor.getIgnoredRequests();

		saveAnnotation(annotation, tag);
		openAnnotation(tag);

		saveApplicationModel(filteredApplication, tag + "-filtered");
		openApplicationModel(tag + "-filtered");

		return "Extracted and opened the annotation and the filtered application model.\nIgnored requests:\n" + ignoredRequests.stream().collect(Collectors.joining("\n"));
	}

	@ShellMethod(key = { "idpa ann check" }, value = "Checks whether the annotation with the specified tag fits to the respective application model.")
	public String checkAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws IOException {
		tag = contextManager.getTagOrFail(tag);

		Application application = readApplicationModel(tag);
		ApplicationAnnotation annotation = readAnnotation(tag);

		AnnotationValidityChecker checker = new AnnotationValidityChecker(application);
		checker.checkAnnotation(annotation);
		AnnotationValidityReport report = checker.getReport();

		return mapper.writeValueAsString(report);
	}

	private Swagger readSwagger(String openApiLocation) {
		Swagger swagger = swaggerParser.read(openApiLocation);

		if (swagger == null) {
			throw new IllegalArgumentException("The OpenAPI at location " + openApiLocation + " could not be found!");
		}

		return swagger;
	}

	private File saveApplicationModel(Application application, String tag) throws JsonGenerationException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + tag + ".yml");

		appSerializer.writeToYaml(application, applicationFile);

		return applicationFile;
	}

	private File saveAnnotation(ApplicationAnnotation annotation, String tag) throws JsonGenerationException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");

		annSerializer.writeToYaml(annotation, annotationFile);

		return annotationFile;
	}

	private Application readApplicationModel(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + tag + ".yml");

		if (applicationFile.exists()) {
			return appSerializer.readFromYaml(applicationFile);
		} else {
			return null;
		}
	}

	private ApplicationAnnotation readAnnotation(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");

		if (annotationFile.exists()) {
			return annSerializer.readFromYaml(annotationFile);
		} else {
			return null;
		}
	}

	private boolean openApplicationModel(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + tag + ".yml");

		if (applicationFile.exists()) {
			Desktop.getDesktop().open(applicationFile);
		}

		return applicationFile.exists();
	}

	private boolean openAnnotation(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");

		if (annotationFile.exists()) {
			Desktop.getDesktop().open(annotationFile);
		}

		return annotationFile.exists();
	}

	private EnumSet<ApplicationChangeType> changeTypesFromBooleans(boolean add, boolean remove, boolean change, boolean endpoints, boolean parameters) {
		EnumSet<ApplicationChangeType> set = EnumSet.noneOf(ApplicationChangeType.class);

		boolean noEntity = !endpoints && !parameters;
		boolean noOperation = !add && !remove && !change;

		change |= noOperation;
		remove |= noOperation;
		add |= noOperation;

		endpoints |= noEntity;
		parameters |= noEntity;

		if (change && endpoints) {
			set.add(ApplicationChangeType.ENDPOINT_CHANGED);
		}

		if (remove && endpoints) {
			set.add(ApplicationChangeType.ENDPOINT_REMOVED);
		}

		if (add && endpoints) {
			set.add(ApplicationChangeType.ENDPOINT_ADDED);
		}

		if (change && parameters) {
			set.add(ApplicationChangeType.PARAMETER_CHANGED);
		}

		if (remove && parameters) {
			set.add(ApplicationChangeType.PARAMETER_REMOVED);
		}

		if (add && parameters) {
			set.add(ApplicationChangeType.PARAMETER_ADDED);
		}

		return set;
	}

}
