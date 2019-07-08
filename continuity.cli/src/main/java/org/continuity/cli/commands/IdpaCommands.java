package org.continuity.cli.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.rest.CustomHeaders;
import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi.Orchestrator.Idpa;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.commons.idpa.AnnotationFromAccessLogsExtractor;
import org.continuity.commons.idpa.AnnotationValidityChecker;
import org.continuity.commons.idpa.ApplicationUpdater;
import org.continuity.commons.idpa.OpenApiToIdpaTransformer;
import org.continuity.commons.utils.FileUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.jline.utils.AttributedString;
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
			new Shorthand("app", this, "goToIdpaAappContext", String.class), //
			new Shorthand("app upload", this, "uploadApplication", String.class), //
			new Shorthand("app create", this, "createIdpaApplication", String.class, String.class), //
			new Shorthand("app update", this, "updateIdpaApplication", String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class), //
			new Shorthand("app init", this, "initApplication", String.class), //
			new Shorthand("ann", this, "goToIdpaAnnContext", String.class), //
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

	private static final String PARAM_VERSION = "version";

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
	public AttributedString goToIdpaContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "idpa download" }, value = "Downloads and opens the IDPA with the specified app-id.")
	public AttributedString downloadIdpa(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws JsonGenerationException, JsonMappingException, IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));

		ResponseEntity<Application> applicationResponse;
		try {
			applicationResponse = restTemplate.getForEntity(Idpa.GET_APPLICATION.requestUrl(aid).withHost(url).withQueryIfNotEmpty(PARAM_VERSION, contextManager.getCurrentVersion()).get(),
					Application.class);
		} catch (HttpStatusCodeException e) {
			applicationResponse = ResponseEntity.status(e.getStatusCode()).body(null);
		}

		ResponseEntity<ApplicationAnnotation> annotationResponse;
		try {
			annotationResponse = restTemplate.getForEntity(Idpa.GET_ANNOTATION.requestUrl(aid).withHost(url).withQueryIfNotEmpty(PARAM_VERSION, contextManager.getCurrentVersion()).get(),
					ApplicationAnnotation.class);
		} catch (HttpStatusCodeException e) {
			annotationResponse = ResponseEntity.status(e.getStatusCode()).body(null);
		}

		ResponseBuilder response = new ResponseBuilder();

		if (applicationResponse.getStatusCode().is2xxSuccessful()) {
			saveApplicationModel(applicationResponse.getBody(), aid);
			openApplicationModel(aid);
		} else if (applicationResponse.getStatusCode().is4xxClientError()) {
			response.bold("There is no such application model!").newline();
		} else {
			response.error("Unknown error when downloading the application model: ").error(applicationResponse.getStatusCode().toString()).error(" (")
					.error(applicationResponse.getStatusCode().getReasonPhrase()).error(")").newline();
		}

		if (annotationResponse.getStatusCode().is2xxSuccessful()) {
			saveAnnotation(annotationResponse.getBody(), aid);
			openAnnotation(aid);

			List<String> brokenValues = annotationResponse.getHeaders().get(CustomHeaders.BROKEN);

			if ((brokenValues != null) && brokenValues.contains("true")) {
				response.error("The annotation is broken! Please use ").boldError("ann check").error(" for details.").newline();
			}
		} else if (annotationResponse.getStatusCode().is4xxClientError()) {
			response.bold("There is no such annotation model!").newline();
		} else {
			response.error("Unknown error when downloading the annotation: ").error(annotationResponse.getStatusCode().toString()).error(" (")
					.error(annotationResponse.getStatusCode().getReasonPhrase()).error(")").newline();
		}

		return response.normal("Downloaded and opened the IDPA with app-id ").normal(aid).normal(" and version ").normal(contextManager.getCurrentVersionOrLatest()).normal(".").build();
	}

	@ShellMethod(key = { "idpa open" }, value = "Opens an already downloaded IDPA with the specified app-id.")
	public String openIdpa(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		boolean appExists = openApplicationModel(aid);
		boolean annExists = openAnnotation(aid);

		StringBuilder response = new StringBuilder();

		if (appExists) {
			response.append("Opened the IDPA application model with app-id ").append(aid);
		} else {
			response.append("The IDPA application model with app-id ").append(aid).append(" does not exist.");
		}

		response.append("\n");

		if (annExists) {
			response.append("Opened the IDPA annotation with app-id ").append(aid);
		} else {
			response.append("The IDPA annotation with app-id ").append(aid).append(" does not exist.");
		}

		return response.toString();
	}

	@ShellMethod(key = { "idpa app" }, value = "Goes to the 'idpa/app' context so that the shorthands can be used.")
	public AttributedString goToIdpaAappContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context, appContext);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "idpa app init" }, value = "Initializes an application model with the specified app-id.")
	public String initApplication(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws JsonParseException, JsonMappingException, IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		Application application = readApplicationModel(aid);

		if (application != null) {
			return "There is already an application model for app-id " + aid + "!";
		} else {
			application = new Application();
			application.setId(aid.toString());
			application.setTimestamp(new Date());

			saveApplicationModel(application, aid);
			openApplicationModel(aid);

			return "Initialized and opened the annotation.";
		}
	}

	@ShellMethod(key = { "idpa app create" }, value = "Creates an IDPA application model from an OpenApi.")
	public String createIdpaApplication(String openApiLocation, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId)
			throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		Swagger swagger = readSwagger(openApiLocation);
		Application application = openApiTransfomer.transform(swagger);
		application.setId(aid.toString());

		saveApplicationModel(application, aid);
		openApplicationModel(aid);

		return "Created and opened the application model with app-id " + aid + " from the OpenAPI at " + openApiLocation;
	}

	@ShellMethod(key = { "idpa app update" }, value = "Updates an IDPA application model from an OpenApi.")
	public String updateIdpaApplication(
			@ShellOption(help = "Location where the OpenAPI model can be found. Can be a URL or a file path. A file name can contain UNIX-like wildcards.") String openApiLocation,
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = "false", value = { "--add", "-a" }, help = "Consider element additions.") boolean add,
			@ShellOption(defaultValue = "false", value = { "--remove", "-r" }, help = "Consider element removals.") boolean remove,
			@ShellOption(defaultValue = "false", value = { "--change", "-c" }, help = "Consider element changes.") boolean change,
			@ShellOption(defaultValue = "false", value = { "--endpoints", "-e" }, help = "Consider endpoints.") boolean endpoints,
			@ShellOption(defaultValue = "false", value = { "--parameters", "-p" }, help = "Consider parameters.") boolean parameters,
			@ShellOption(defaultValue = "false", value = { "--hide-ignored" }, help = "Consider parameters.") boolean hideIgnored)
			throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {

		StringBuilder response = new StringBuilder();

		AppId aid = contextManager.getAppIdOrFail(appId);
		Application origApplication = readApplicationModel(aid);
		Application updatedApplication = origApplication;
		EnumSet<ApplicationChangeType> changeTypes = changeTypesFromBooleans(add, remove, change, endpoints, parameters);

		if (!openApiLocation.startsWith("http")) {
			for (File openApiFile : FileUtils.getAllFilesMatchingWildcards(openApiLocation)) {
				ApplicationChangeReport report = updateApplicationModel(updatedApplication, openApiFile.getPath(), aid, changeTypes);
				updatedApplication = report.getUpdatedApplication();
				report.setUpdatedApplication(null);

				if (hideIgnored) {
					report.setIgnoredApplicationChanges(null);
				}

				response.append("Updated with ").append(openApiFile.getPath()).append(":\n").append(mapper.writeValueAsString(report));
			}
		} else {
			ApplicationChangeReport report = updateApplicationModel(origApplication, openApiLocation, aid, changeTypes);
			updatedApplication = report.getUpdatedApplication();
			report.setUpdatedApplication(null);

			if (hideIgnored) {
				report.setIgnoredApplicationChanges(null);
			}

			response.append("Updated with ").append(openApiLocation).append(":\n").append(mapper.writeValueAsString(report));
		}

		AppId origAid = AppId.fromString(aid + "_old");
		origApplication.setId(origAid.toString());

		saveApplicationModel(origApplication, origAid);
		saveApplicationModel(updatedApplication, aid);

		openApplicationModel(origAid);
		openApplicationModel(aid);

		return response.toString();
	}

	private ApplicationChangeReport updateApplicationModel(Application origApplication, String openApiLocation, AppId aid, EnumSet<ApplicationChangeType> changeTypes)
			throws JsonGenerationException, JsonMappingException, IOException {
		Swagger swagger = readSwagger(openApiLocation);

		Application newApplication = openApiTransfomer.transform(swagger);

		ApplicationChangeReport report = applicationUpdater.updateApplication(origApplication, newApplication, changeTypes);

		AppId origAid = AppId.fromString(aid + "_old");
		origApplication.setId(origAid.toString());

		saveApplicationModel(origApplication, origAid);
		saveApplicationModel(report.getUpdatedApplication(), aid);

		openApplicationModel(origAid);
		openApplicationModel(aid);

		return report;
	}

	@SuppressWarnings("unchecked")
	@ShellMethod(key = { "idpa app upload" }, value = "Uploads the application model with the specified app-id. Can break the online stored annotation!")
	public AttributedString uploadApplication(
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE, help = "App-id of the application model. Can contain UNIX-like wildcards.") String pattern)
			throws JsonParseException, JsonMappingException, IOException {
		AppId aidPattern = contextManager.getAppIdOrFail(pattern);

		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		ResponseEntity<String> response;
		List<AppId> aids = new ArrayList<>();
		ResponseBuilder responses = new ResponseBuilder();
		boolean error = false;

		for (File file : FileUtils.getAllFilesMatchingWildcards(workingDir + "/application-" + aidPattern + ".yml")) {
			Application application = appSerializer.readFromYaml(file);
			String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));
			String appId = file.getName().substring("application-".length(), file.getName().length() - ".yml".length());
			aids.add(AppId.fromString(appId));
			try {
				response = restTemplate.postForEntity(Idpa.UPDATE_APPLICATION.requestUrl(appId).withHost(url).get(), application, String.class);
			} catch (HttpStatusCodeException e) {
				response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
			}

			responses.newline().bold(appId);

			if (!response.getStatusCode().is2xxSuccessful()) {
				responses.error(" [ERROR]");
			}

			responses.newline();

			List<String> broken = null;

			try {
				broken = restTemplate.getForObject(Idpa.GET_BROKEN.requestUrl(appId).withQuery(PARAM_VERSION, application.getVersionOrTimestamp().toString()).withHost(url).get(), List.class);
			} catch (HttpStatusCodeException e) {
				responses.error("Error when checking broken annotations! ").boldError(e.getStatusCode()).error(" (").error(e.getStatusCode().getReasonPhrase()).error(") - ")
						.error(e.getResponseBodyAsString()).newline();
			}

			if ((broken != null) && (broken.size() > 0)) {
				responses.error("The new application version broke the following annotations: ");
				responses.error(broken.stream().collect(Collectors.joining(", "))).newline();
			}

			if (response.getStatusCode().is2xxSuccessful()) {
				responses.jsonAsYamlNormal(response.getBody());
			} else {
				responses.jsonAsYamlError(response.getBody());
				error = true;
			}
		}

		if (error) {
			return new ResponseBuilder().normal("Uploaded application models for app-ids ").normal(aids).normal(". ").error("Some of them resulted in errors:").newline().append(responses).build();
		} else {
			return new ResponseBuilder().normal("Successfully uploaded application models for app-ids ").normal(aids).normal(":").newline().append(responses).build();
		}

	}

	@ShellMethod(key = { "idpa ann" }, value = "Goes to the 'idpa/ann' context so that the shorthands can be used.")
	public AttributedString goToIdpaAnnContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context, annContext);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "idpa ann upload" }, value = "Uploads the annotation with the specified app-id.")
	public AttributedString uploadAnnotation(
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE, help = "App-ids of the annotation. Can contain UNIX-like wildcards.") String pattern)
			throws JsonParseException, JsonMappingException, IOException, NumberFormatException, ParseException {
		AppId aidPattern = contextManager.getAppIdOrFail(pattern);

		ResponseBuilder resp = new ResponseBuilder();

		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		List<AppId> aids = new ArrayList<>();
		ResponseBuilder responses = new ResponseBuilder();
		boolean error = false;

		String currVersion = contextManager.getCurrentVersion();

		if (currVersion == null) {
			currVersion = ApiFormats.DATE_FORMAT.format(new Date());
			resp.normal("No version set! Using the current time as fallback: ").bold(currVersion).newline();
		}

		for (File file : FileUtils.getAllFilesMatchingWildcards(workingDir + "/annotation-" + aidPattern + ".yml")) {
			ApplicationAnnotation annotation = annSerializer.readFromYaml(file);
			String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));
			String appId = file.getName().substring("annotation-".length(), file.getName().length() - ".yml".length());
			aids.add(AppId.fromString(appId));

			RequestBuilder req = Idpa.UPDATE_ANNOTATION.requestUrl(appId).withHost(url);

			if (annotation.getVersionOrTimestamp().isEmpty()) {
				resp.bold("Annotation '").normal(appId).bold("' has no version! Setting the current one as fallback: ").normal(currVersion).newline();
				annotation.setVersionOrTimestamp(VersionOrTimestamp.fromString(currVersion));
			}

			ResponseEntity<String> response;
			try {
				response = restTemplate.postForEntity(req.get(), annotation, String.class);
			} catch (HttpStatusCodeException e) {
				response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
			}

			responses.newline().bold(appId);

			if (response.getStatusCode().is2xxSuccessful()) {
				responses.newline().jsonAsYamlNormal(response.getBody());
			} else {
				responses.error(" [ERROR]").newline().jsonAsYamlError(response.getBody());
				error = true;
			}
		}

		if (error) {
			return resp.normal("Uploaded annotations for app-ids ").normal(aids).normal(". ").error("Some of them resulted in errors:").newline().append(responses).build();
		} else {
			return resp.normal("Successfully uploaded annotations for app-ids ").normal(aids).normal(":").newline().append(responses).build();
		}
	}

	@ShellMethod(key = { "idpa ann init" }, value = "Initializes an annotation for the stored application model with the specified app-id.")
	public String initAnnotation(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws JsonParseException, JsonMappingException, IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		Application application = readApplicationModel(aid);
		ApplicationAnnotation annotation = new AnnotationExtractor().extractAnnotation(application);

		saveAnnotation(annotation, aid);

		openApplicationModel(aid);
		openAnnotation(aid);

		return "Initialized and opened the annotation.";
	}

	@ShellMethod(key = { "idpa ann extract" }, value = "Extracts an annotation for the stored application model with the specified app-id from Apache request logs.")
	public String extractAnnotation(String logsFile, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = AccessLogEntry.DEFAULT_REGEX, help = "The regular expression used to extract the request method and path including the query. There should be one capture group per property in the mentioned order.") String regex)
			throws IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		Application application = readApplicationModel(aid);
		ApplicationAnnotation annotation = readAnnotation(aid);

		if (application == null) {
			return "There is no application model! Please create one first.";
		}

		if (annotation != null) {
			return "There is already an annotation! Remove or move it first before extracting a new one.";
		}

		Path pathToLogs = Paths.get(logsFile);
		Path workingDir = Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR));

		if (pathToLogs.isAbsolute()) {
			pathToLogs = workingDir.resolve(pathToLogs);
		}

		AnnotationFromAccessLogsExtractor extractor = new AnnotationFromAccessLogsExtractor(application, pathToLogs, workingDir);
		extractor.setRegex(regex);
		extractor.extract();

		annotation = extractor.getExtractedAnnotation();
		Application filteredApplication = extractor.getFilteredApplication();
		Set<String> ignoredRequests = extractor.getIgnoredRequests();

		saveAnnotation(annotation, aid);
		openAnnotation(aid);

		saveApplicationModel(filteredApplication, AppId.fromString(aid + "-filtered"));
		openApplicationModel(AppId.fromString(aid + "-filtered"));

		return "Extracted and opened the annotation and the filtered application model.\nIgnored requests:\n" + ignoredRequests.stream().collect(Collectors.joining("\n"));
	}

	@ShellMethod(key = { "idpa ann check" }, value = "Checks whether the annotation with the specified app-id fits to the respective application model.")
	public String checkAnnotation(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		Application application = readApplicationModel(aid);
		ApplicationAnnotation annotation = readAnnotation(aid);

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

	private File saveApplicationModel(Application application, AppId aid) throws JsonGenerationException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + aid + ".yml");

		appSerializer.writeToYaml(application, applicationFile);

		return applicationFile;
	}

	private File saveAnnotation(ApplicationAnnotation annotation, AppId aid) throws JsonGenerationException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File annotationFile = new File(workingDir + "/annotation-" + aid + ".yml");

		annSerializer.writeToYaml(annotation, annotationFile);

		return annotationFile;
	}

	private Application readApplicationModel(AppId aid) throws IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + aid + ".yml");

		if (applicationFile.exists()) {
			return appSerializer.readFromYaml(applicationFile);
		} else {
			return null;
		}
	}

	private ApplicationAnnotation readAnnotation(AppId aid) throws IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File annotationFile = new File(workingDir + "/annotation-" + aid + ".yml");

		if (annotationFile.exists()) {
			return annSerializer.readFromYaml(annotationFile);
		} else {
			return null;
		}
	}

	private boolean openApplicationModel(AppId aid) throws IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + aid + ".yml");

		if (applicationFile.exists()) {
			Desktop.getDesktop().open(applicationFile);
		}

		return applicationFile.exists();
	}

	private boolean openAnnotation(AppId aid) throws IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File annotationFile = new File(workingDir + "/annotation-" + aid + ".yml");

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
