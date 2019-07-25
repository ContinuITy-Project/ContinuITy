package org.continuity.cli.storage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.exception.CliException;
import org.continuity.commons.utils.FileUtils;
import org.continuity.idpa.AppId;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;

public class IdpaStorage {

	private final DirectoryManager directory;

	private final IdpaYamlSerializer<Application> appSerializer = new IdpaYamlSerializer<>(Application.class);

	private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

	public IdpaStorage(PropertiesProvider properties) {
		this.directory = new DirectoryManager("idpa", properties);
	}

	public Path store(Application application, AppId aid) throws IOException {
		Path path = getApplicationPath(aid, true);
		appSerializer.writeToYaml(application, path);
		return path;
	}

	public Path store(ApplicationAnnotation annotation, AppId aid) throws IOException {
		Path path = getAnnotationPath(aid, true);
		annSerializer.writeToYaml(annotation, path);
		return path;
	}

	public Path storeIfNotPresent(Application application, AppId aid) throws IOException, CliException {
		Path path = getAnnotationPath(aid, true);

		if (path.toFile().exists()) {
			throw new CliException("There is already an application! Remove or move it first before storing a new one.");
		}

		appSerializer.writeToYaml(application, path);
		return path;
	}

	public Path storeIfNotPresent(ApplicationAnnotation annotation, AppId aid) throws IOException, CliException {
		Path path = getAnnotationPath(aid, true);

		if (path.toFile().exists()) {
			throw new CliException("There is already an annotation! Remove or move it first before storing a new one.");
		}

		annSerializer.writeToYaml(annotation, path);
		return path;
	}

	public boolean openApplication(AppId aid) throws IOException {
		File applicationFile = getApplicationPath(aid, false).toFile();

		if (applicationFile.exists()) {
			Desktop.getDesktop().open(applicationFile);
		}

		return applicationFile.exists();
	}

	public boolean openAnnotation(AppId aid) throws IOException {
		File annotationFile = getAnnotationPath(aid, false).toFile();

		if (annotationFile.exists()) {
			Desktop.getDesktop().open(annotationFile);
		}

		return annotationFile.exists();
	}

	public boolean applicationExists(AppId aid) {
		Path path = getApplicationPath(aid, false);
		return path.toFile().exists();
	}

	public boolean annotationExists(AppId aid) {
		Path path = getAnnotationPath(aid, false);
		return path.toFile().exists();
	}

	public Application readApplication(AppId aid) throws IOException, CliException {
		Path path = getApplicationPath(aid, false);

		if (path.toFile().exists()) {
			return appSerializer.readFromYaml(path);
		} else {
			throw new CliException("There is no application! Please create one first.");
		}
	}

	public ApplicationAnnotation readAnnotation(AppId aid) throws IOException, CliException {
		Path path = getAnnotationPath(aid, false);

		if (path.toFile().exists()) {
			return annSerializer.readFromYaml(path);
		} else {
			throw new CliException("There is no annotation! Please create one first.");
		}
	}

	public Stream<Pair<AppId, Application>> listApplications(AppId appIdPattern) {
		return FileUtils.getAllFilesMatchingWildcards(getApplicationPath(appIdPattern, false).toString()).stream().map(this::readApplicationPair).filter(Objects::nonNull);
	}

	public Stream<Pair<AppId, ApplicationAnnotation>> listAnnotations(AppId appIdPattern) {
		return FileUtils.getAllFilesMatchingWildcards(getAnnotationPath(appIdPattern, false).toString()).stream().map(this::readAnnotationPair).filter(Objects::nonNull);
	}

	private Pair<AppId, Application> readApplicationPair(File file) {
		Application app;

		try {
			app = appSerializer.readFromYaml(file);
		} catch (IOException e) {
			return null;
		}

		String appId = file.getName().substring("application-".length(), file.getName().length() - ".yml".length());
		return Pair.of(AppId.fromString(appId), app);
	}

	private Pair<AppId, ApplicationAnnotation> readAnnotationPair(File file) {
		ApplicationAnnotation ann;

		try {
			ann = annSerializer.readFromYaml(file);
		} catch (IOException e) {
			return null;
		}

		String appId = file.getName().substring("annotation-".length(), file.getName().length() - ".yml".length());
		return Pair.of(AppId.fromString(appId), ann);
	}

	private Path getApplicationPath(AppId aid, boolean create) {
		return directory.getDir(aid, create).resolve("application-" + aid + ".yml");
	}

	private Path getAnnotationPath(AppId aid, boolean create) {
		return directory.getDir(aid, create).resolve("annotation-" + aid + ".yml");
	}

}
