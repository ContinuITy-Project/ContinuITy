package org.continuity.cli.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.exception.CliException;
import org.continuity.cli.process.JMeterProcess;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.jmeter.TestPlanWriter;
import org.continuity.idpa.AppId;

import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

public class JMeterStorage {

	public static final String LINK_LATEST = "LATEST";

	public static final String KEY_JMETER_HOME = "jmeter.home";

	private final CSVHandler csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

	private final OrderDirectoryManager directory;

	private final PropertiesProvider properties;

	private TestPlanWriter testPlanWriter;

	private String jmeterHome;

	public JMeterStorage(PropertiesProvider properties) {
		this.directory = new OrderDirectoryManager("jmeter", properties);
		this.properties = properties;
	}

	public void init() throws CliException {
		String newHome = properties.getProperty(KEY_JMETER_HOME);

		if (newHome == null) {
			throw new CliException(new ResponseBuilder().error("Please set the jmeter home path first (call ").boldError("jmeter home [path]").error(")").build());
		} else if (!newHome.equals(jmeterHome)) {
			testPlanWriter = new TestPlanWriter(newHome);
			jmeterHome = newHome;
		}
	}

	public Path store(JMeterTestPlanBundle bundle, AppId aid, String jmeterLink) {
		String orderId = RestApi.JMeter.TestPlan.GET.parsePathParameters(jmeterLink).get(0);
		Path testPlanDir = directory.getFreshDir(aid, orderId);

		return testPlanWriter.write(bundle.getTestPlan(), bundle.getBehaviors(), testPlanDir);
	}

	public Path storeAndOpen(JMeterTestPlanBundle bundle, AppId aid, String jmeterLink) throws IOException {
		Path path = store(bundle, aid, jmeterLink);

		new JMeterProcess(properties.getProperty(KEY_JMETER_HOME)).run(path);

		return path;
	}

	public Path open(AppId aid, String jmeterLink) throws IOException {
		Path path;

		if (LINK_LATEST.equals(jmeterLink)) {
			path = directory.getLatest(aid);
		} else {
			String orderId = RestApi.JMeter.TestPlan.GET.parsePathParameters(jmeterLink).get(0);
			path = directory.getDir(aid, orderId, false);
		}

		if (path.toFile().exists()) {
			new JMeterProcess(properties.getProperty(KEY_JMETER_HOME)).run(path.resolve("testplan.jmx"));
		}

		return path;
	}

	public JMeterTestPlanBundle read(String loadTestPath) throws IOException, CliException {
		String workingDir = properties.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		Path testPlanDir = Paths.get(workingDir).resolve(loadTestPath);

		File dir = testPlanDir.toFile();

		if (!dir.exists() || !dir.isDirectory()) {
			throw new CliException(new ResponseBuilder().error(testPlanDir.toAbsolutePath().toString()).error(" is not a directory!").build());
		}

		ListedHashTree testPlan = null;
		Map<String, String[][]> behaviors = new HashMap<>();

		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".jmx")) {
				testPlan = (ListedHashTree) SaveService.loadTree(file);
			} else if (file.getName().endsWith(".csv")) {
				behaviors.put(file.getName(), csvHandler.readValues(file.getAbsolutePath()));
			}
		}

		if (testPlan == null) {
			throw new CliException(new ResponseBuilder().error("No .jmx test plan found!").build());
		}

		return new JMeterTestPlanBundle(testPlan, behaviors);
	}

}
