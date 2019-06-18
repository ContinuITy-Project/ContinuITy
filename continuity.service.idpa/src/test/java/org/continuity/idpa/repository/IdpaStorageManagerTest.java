package org.continuity.idpa.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.continuity.idpa.SystemModelTestInstance.FIRST;
import static org.continuity.idpa.SystemModelTestInstance.FIRST_REFINED;
import static org.continuity.idpa.SystemModelTestInstance.SECOND;
import static org.continuity.idpa.SystemModelTestInstance.TAG;
import static org.continuity.idpa.SystemModelTestInstance.THIRD;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.SystemModelTestInstance;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.continuity.idpa.storage.AnnotationStorageManager;
import org.continuity.idpa.storage.ApplicationStorageManager;
import org.continuity.idpa.storage.IdpaStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author Henning Schulz
 *
 */
public class IdpaStorageManagerTest {

	private IdpaYamlSerializer<IdpaElement> serializer = new IdpaYamlSerializer<>(IdpaElement.class);

	private Path storageDir;

	private IdpaStorage storage;

	private ApplicationStorageManager appManager;

	private AnnotationStorageManager annManager;

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
		addAndCheck(FIRST);
		addAndCheck(FIRST_REFINED, FIRST);
		addAndCheck(SECOND);
		addAndCheck(THIRD);

		checkResult(true);
	}

	@Test
	public void testMixed() throws JsonProcessingException, IOException {
		appManager.saveOrUpdate(TAG, SECOND.getApplication());
		appManager.saveOrUpdate(TAG, FIRST.getApplication());
		annManager.saveOrUpdate(TAG, FIRST.getApplication().getTimestamp(), FIRST.getAnnotation());
		appManager.saveOrUpdate(TAG, THIRD.getApplication());
		annManager.saveOrUpdate(TAG, SECOND.getApplication().getTimestamp(), SECOND.getAnnotation());
		annManager.saveOrUpdate(TAG, THIRD.getApplication().getTimestamp(), THIRD.getAnnotation());
		appManager.saveOrUpdate(TAG, FIRST_REFINED.getApplication());
		annManager.saveOrUpdate(TAG, FIRST_REFINED.getApplication().getTimestamp(), FIRST_REFINED.getAnnotation());

		checkResult(true);
	}

	@Test
	public void testWithOverwriting() throws JsonProcessingException, IOException {
		appManager.saveOrUpdate(TAG, SECOND.getApplication());
		appManager.saveOrUpdate(TAG, FIRST.getApplication());
		annManager.saveOrUpdate(TAG, FIRST.getApplication().getTimestamp(), FIRST.getAnnotation());
		appManager.saveOrUpdate(TAG, THIRD.getApplication());
		annManager.saveOrUpdate(TAG, SECOND.getApplication().getTimestamp(), SECOND.getAnnotation());
		annManager.saveOrUpdate(TAG, THIRD.getApplication().getTimestamp(), THIRD.getAnnotation());
		appManager.saveOrUpdate(TAG, FIRST_REFINED.getApplication());
		annManager.saveOrUpdate(TAG, FIRST.getApplication().getTimestamp(), FIRST_REFINED.getAnnotation());

		checkResult(false);
	}

	@Test
	public void testWithBroken() throws IOException {
		appManager.saveOrUpdate(TAG, FIRST.getApplication());
		annManager.saveOrUpdate(TAG, FIRST.getApplication().getTimestamp(), FIRST.getAnnotation());
		annManager.saveOrUpdate(TAG, FIRST_REFINED.getApplication().getTimestamp(), FIRST_REFINED.getAnnotation());

		Application breakingApp = SECOND.getApplication();
		breakingApp.setTimestamp(FIRST_REFINED.getApplication().getTimestamp());
		appManager.saveOrUpdate(TAG, breakingApp);

		annManager.saveOrUpdate(TAG, SECOND.getApplication().getTimestamp(), SECOND.getAnnotation());


		checkApp(FIRST, FIRST, false);
		checkApp(FIRST, FIRST, true);

		checkApp(FIRST_REFINED, breakingApp, false);
		checkApp(FIRST_REFINED, breakingApp, true);

		checkApp(SECOND, breakingApp, false);
		checkApp(SECOND, breakingApp, true);

		checkAnn(FIRST, FIRST, false);
		checkAnn(FIRST, FIRST, true);
		assertThat(annManager.isBroken(TAG, FIRST.getApplication().getTimestamp())).isFalse();

		checkAnn(FIRST_REFINED, FIRST_REFINED, false);
		checkAnn(FIRST_REFINED, FIRST_REFINED, true);
		assertThat(annManager.isBroken(TAG, FIRST_REFINED.getApplication().getTimestamp())).isTrue();

		checkAnn(SECOND, SECOND, false);
		checkAnn(SECOND, SECOND, true);
		assertThat(annManager.isBroken(TAG, SECOND.getApplication().getTimestamp())).isFalse();
	}

	private void addAndCheck(SystemModelTestInstance inst) throws JsonProcessingException, IOException {
		addAndCheck(inst, inst, inst);
	}

	private void addAndCheck(SystemModelTestInstance inst, SystemModelTestInstance appCheck) throws JsonProcessingException, IOException {
		addAndCheck(inst, appCheck, inst);
	}

	private void addAndCheck(SystemModelTestInstance inst, SystemModelTestInstance appCheck, SystemModelTestInstance annCheck) throws JsonProcessingException, IOException {
		appManager.saveOrUpdate(TAG, inst.getApplication());
		checkEquality(appManager.read(TAG), appCheck.getApplication());
		annManager.saveOrUpdate(TAG, inst.getApplication().getTimestamp(), inst.getAnnotation());
		checkEquality(annManager.read(TAG), annCheck.getAnnotation());
	}

	private void checkApp(SystemModelTestInstance timestampHolder, SystemModelTestInstance expected, boolean delay) throws JsonProcessingException, IOException {
		checkApp(timestampHolder, expected.getApplication(), delay);
	}

	private void checkApp(SystemModelTestInstance timestampHolder, Application expected, boolean delay) throws JsonProcessingException, IOException {
		Date timestamp = timestampHolder.getApplication().getTimestamp();

		if (delay) {
			timestamp = DateUtils.addSeconds(timestamp, 10);
		}

		checkEquality(appManager.read(TAG, timestamp), expected);
	}

	private void checkAnn(SystemModelTestInstance timestampHolder, SystemModelTestInstance expected, boolean delay) throws JsonProcessingException, IOException {
		Date timestamp = timestampHolder.getApplication().getTimestamp();

		if (delay) {
			timestamp = DateUtils.addSeconds(timestamp, 10);
		}

		checkEquality(annManager.read(TAG, timestamp), expected.getAnnotation());
	}

	private void checkEquality(IdpaElement tested, IdpaElement orig) throws JsonProcessingException {
		assertThat(serializer.writeToYamlString(tested)).isEqualTo(serializer.writeToYamlString(orig));
	}

	private void checkResult(boolean refinedSeparate) throws JsonProcessingException, IOException {
		checkApp(FIRST, FIRST, false);
		checkApp(FIRST_REFINED, FIRST, false);
		checkApp(SECOND, SECOND, false);
		checkApp(THIRD, THIRD, false);
		checkApp(FIRST, FIRST, true);
		checkApp(FIRST_REFINED, FIRST, true);
		checkApp(SECOND, SECOND, true);
		checkApp(THIRD, THIRD, true);

		checkAnn(FIRST, refinedSeparate ? FIRST : FIRST_REFINED, false);
		checkAnn(FIRST_REFINED, FIRST_REFINED, false);
		checkAnn(SECOND, SECOND, false);
		checkAnn(THIRD, THIRD, false);
		checkAnn(FIRST, refinedSeparate ? FIRST : FIRST_REFINED, true);
		checkAnn(FIRST_REFINED, FIRST_REFINED, true);
		checkAnn(SECOND, SECOND, true);
		checkAnn(THIRD, THIRD, true);
	}

}
