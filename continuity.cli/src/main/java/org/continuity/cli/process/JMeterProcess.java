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

	public JMeterProcess(String jmeterHome) {
		this.jmeterHome = jmeterHome;
	}

	public void run(Path testPlanPath) throws IOException {
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(jmeterHome + "/bin/jmeter.bat -t " + testPlanPath.toString());
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
