package org.continuity.idpa.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.continuity.idpa.StaticIdpaTestInstance.APP_ID;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.IdpaTestInstance;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class IdpaStorageManagerTest {

	private IdpaYamlSerializer<IdpaElement> serializer = new IdpaYamlSerializer<>(IdpaElement.class);

	private Path storageDir;

	private IdpaStorage storage;

	private ApplicationStorageManager appManager;

	private AnnotationStorageManager annManager;

	protected abstract IdpaTestInstance first();

	protected abstract IdpaTestInstance first_refined();

	protected abstract IdpaTestInstance second();

	protected abstract IdpaTestInstance third();

	protected abstract VersionOrTimestamp addSmallOffset(VersionOrTimestamp version);

	@Before
	public void setup() {
		storageDir = Paths.get("IdpaStorageManagerTest-" + new Random().nextInt());
		storage = new IdpaStorage(storageDir);

		appManager = new ApplicationStorageManager(storage);
		annManager = new AnnotationStorageManager(storage);
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(storageDir.toFile());
	}

	@Test
	public void testInARow() throws IOException {
		addAndCheck(first());
		addAndCheck(first_refined(), first());
		addAndCheck(second());
		addAndCheck(third());

		checkResult(true);
	}

	@Test
	public void testMixed() throws JsonProcessingException, IOException {
		appManager.saveOrUpdate(APP_ID, second().getApplication());
		appManager.saveOrUpdate(APP_ID, first().getApplication());
		annManager.saveOrUpdate(APP_ID, first().getApplication().getVersionOrTimestamp(), first().getAnnotation());
		appManager.saveOrUpdate(APP_ID, third().getApplication());
		annManager.saveOrUpdate(APP_ID, second().getApplication().getVersionOrTimestamp(), second().getAnnotation());
		annManager.saveOrUpdate(APP_ID, third().getApplication().getVersionOrTimestamp(), third().getAnnotation());
		appManager.saveOrUpdate(APP_ID, first_refined().getApplication());
		annManager.saveOrUpdate(APP_ID, first_refined().getApplication().getVersionOrTimestamp(), first_refined().getAnnotation());

		checkResult(true);
	}

	@Test
	public void testWithOverwriting() throws JsonProcessingException, IOException {
		appManager.saveOrUpdate(APP_ID, second().getApplication());
		appManager.saveOrUpdate(APP_ID, first().getApplication());
		annManager.saveOrUpdate(APP_ID, first().getApplication().getVersionOrTimestamp(), first().getAnnotation());
		appManager.saveOrUpdate(APP_ID, third().getApplication());
		annManager.saveOrUpdate(APP_ID, second().getApplication().getVersionOrTimestamp(), second().getAnnotation());
		annManager.saveOrUpdate(APP_ID, third().getApplication().getVersionOrTimestamp(), third().getAnnotation());
		appManager.saveOrUpdate(APP_ID, first_refined().getApplication());
		annManager.saveOrUpdate(APP_ID, first().getApplication().getVersionOrTimestamp(), first_refined().getAnnotation());

		checkResult(false);
	}

	@Test
	public void testWithBroken() throws IOException {
		appManager.saveOrUpdate(APP_ID, first().getApplication());
		annManager.saveOrUpdate(APP_ID, first().getApplication().getVersionOrTimestamp(), first().getAnnotation());
		annManager.saveOrUpdate(APP_ID, first_refined().getApplication().getVersionOrTimestamp(), first_refined().getAnnotation());

		Application breakingApp = second().getApplication();
		breakingApp.setVersionOrTimestamp(first_refined().getApplication().getVersionOrTimestamp());
		appManager.saveOrUpdate(APP_ID, breakingApp);

		annManager.saveOrUpdate(APP_ID, second().getApplication().getVersionOrTimestamp(), second().getAnnotation());


		checkApp(first(), first(), false);
		checkApp(first(), first(), true);

		checkApp(first_refined(), breakingApp, false);
		checkApp(first_refined(), breakingApp, true);

		checkApp(second(), breakingApp, false);
		checkApp(second(), breakingApp, true);

		checkAnn(first(), first(), false);
		checkAnn(first(), first(), true);
		assertThat(annManager.isBroken(APP_ID, first().getApplication().getVersionOrTimestamp())).isFalse();

		checkAnn(first_refined(), first_refined(), false);
		checkAnn(first_refined(), first_refined(), true);
		assertThat(annManager.isBroken(APP_ID, first_refined().getApplication().getVersionOrTimestamp())).isTrue();

		checkAnn(second(), second(), false);
		checkAnn(second(), second(), true);
		assertThat(annManager.isBroken(APP_ID, second().getApplication().getVersionOrTimestamp())).isFalse();
	}

	private void addAndCheck(IdpaTestInstance inst) throws JsonProcessingException, IOException {
		addAndCheck(inst, inst, inst);
	}

	private void addAndCheck(IdpaTestInstance inst, IdpaTestInstance appCheck) throws JsonProcessingException, IOException {
		addAndCheck(inst, appCheck, inst);
	}

	private void addAndCheck(IdpaTestInstance inst, IdpaTestInstance appCheck, IdpaTestInstance annCheck) throws JsonProcessingException, IOException {
		appManager.saveOrUpdate(APP_ID, inst.getApplication());
		checkEquality(appManager.read(APP_ID), appCheck.getApplication());
		annManager.saveOrUpdate(APP_ID, inst.getApplication().getVersionOrTimestamp(), inst.getAnnotation());
		checkEquality(annManager.read(APP_ID), annCheck.getAnnotation());
	}

	private void checkApp(IdpaTestInstance timestampHolder, IdpaTestInstance expected, boolean delay) throws JsonProcessingException, IOException {
		checkApp(timestampHolder, expected.getApplication(), delay);
	}

	private void checkApp(IdpaTestInstance timestampHolder, Application expected, boolean delay) throws JsonProcessingException, IOException {
		VersionOrTimestamp timestamp = timestampHolder.getApplication().getVersionOrTimestamp();

		if (delay) {
			timestamp = addSmallOffset(timestamp);
		}

		checkEquality(appManager.read(APP_ID, timestamp), expected);
	}

	private void checkAnn(IdpaTestInstance timestampHolder, IdpaTestInstance expected, boolean delay) throws JsonProcessingException, IOException {
		checkAnn(timestampHolder, expected.getAnnotation(), delay);
	}

	private void checkAnn(IdpaTestInstance timestampHolder, ApplicationAnnotation expected, boolean delay) throws JsonProcessingException, IOException {
		VersionOrTimestamp timestamp = timestampHolder.getApplication().getVersionOrTimestamp();

		if (delay) {
			timestamp = addSmallOffset(timestamp);
		}

		checkEquality(annManager.read(APP_ID, timestamp), expected);
	}

	private void checkEquality(IdpaElement tested, IdpaElement orig) throws JsonProcessingException {
		assertThat(serializer.writeToYamlString(tested)).isEqualTo(serializer.writeToYamlString(orig));
	}

	private void checkResult(boolean refinedSeparate) throws JsonProcessingException, IOException {
		checkApp(first(), first(), false);
		checkApp(first_refined(), first(), false);
		checkApp(second(), second(), false);
		checkApp(third(), third(), false);
		checkApp(first(), first(), true);
		checkApp(first_refined(), first(), true);
		checkApp(second(), second(), true);
		checkApp(third(), third(), true);

		if (refinedSeparate) {
			checkAnn(first(), first(), false);
			checkAnn(first(), first(), true);
			checkAnn(first_refined(), first_refined(), false);
			checkAnn(first_refined(), first_refined(), true);
		} else {
			ApplicationAnnotation ann = first_refined().getAnnotation();
			ann.setVersionOrTimestamp(first().getAnnotation().getVersionOrTimestamp());
			checkAnn(first(), ann, false);
			checkAnn(first(), ann, true);
			checkAnn(first_refined(), ann, false);
			checkAnn(first_refined(), ann, true);
		}

		checkAnn(second(), second(), false);
		checkAnn(third(), third(), false);
		checkAnn(second(), second(), true);
		checkAnn(third(), third(), true);
	}

}
