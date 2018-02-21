package org.continuity.cli.process;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

/**
 * @author Henning Schulz
 *
 */
public class JMeterProcess {

	private final String jmeterHome;

	private final String command;

	public JMeterProcess(String jmeterHome) {
		this.jmeterHome = jmeterHome;

		String os = System.getProperty("os.name");

		if (os.startsWith("Windows")) {
			this.command = "jmeter.bat";
		} else {
			this.command = "jmeter";
		}
	}

	public void run(Path testPlanPath) throws IOException {
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(jmeterHome + "/bin/" + command + " -t " + testPlanPath.toString());
		IOUtils.copy(pr.getInputStream(), System.out);
	}

	public void start(Path testPlanPath) {
		new Thread(() -> {
			try {
				run(testPlanPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

}
