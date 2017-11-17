package org.continuity.jmeter.amqp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.entities.TestPlanPack;
import org.continuity.jmeter.io.TestPlanWriter;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@Component
public class IncomingTestPlanAmqpHandler {

	private String jmeterHome = "C:/apache-jmeter-3.0/apache-jmeter-3.0";

	@Autowired
	private TestPlanWriter testPlanWriter;

	@Autowired
	private RestTemplate restTemplate;

	@RabbitListener(queues = RabbitMqConfig.LOAD_TEST_CREATED_QUEUE_NAME)
	public void handleIncomingTestPlan(TestPlanPack testPlanPack) {
		System.out.println("Received test plan.");

		ListedHashTree annotatedTestPlan = createAnnotatedTestPlan(testPlanPack);

		if (annotatedTestPlan == null) {
			// TODO: log error
			System.err.println("ERROR: Could not annotate test plan");
			annotatedTestPlan = testPlanPack.getTestPlan();
		}

		Path tmpPath;

		try {
			tmpPath = Files.createTempDirectory("jmeter-test-plan");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Path testPlanPath = testPlanWriter.write(annotatedTestPlan, testPlanPack.getBehaviors(), tmpPath);
		System.out.println("Wrote test plan to " + testPlanPath);

		// TODO: remove
		try {
			startJMeter(testPlanPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run the test:
		// StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
		// jmeterEngine.configure(annotatedTestPlan);
		// jmeterEngine.run();
	}

	private ListedHashTree createAnnotatedTestPlan(TestPlanPack testPlanPack) {
		SystemAnnotation annotation;
		try {
			annotation = restTemplate.getForObject(addProtocolIfMissing(testPlanPack.getAnnotationLink() + "/annotation"), SystemAnnotation.class);
		} catch (RestClientException e) {
			e.printStackTrace();
			return null;
		}

		if (annotation == null) {
			// TODO: Print error message
			System.err.println("Annotation at " + testPlanPack.getAnnotationLink() + "/annotation is null!");
			return null;
		}

		SystemModel systemModel = restTemplate.getForObject(addProtocolIfMissing(testPlanPack.getAnnotationLink() + "/system"), SystemModel.class);

		if (systemModel == null) {
			// TODO: Print error message
			System.err.println("System at " + testPlanPack.getAnnotationLink() + "/system is null!");
			return null;
		}

		ListedHashTree testPlan = testPlanPack.getTestPlan();
		JMeterAnnotator annotator = new JMeterAnnotator(testPlan, systemModel);
		annotator.addAnnotations(annotation);

		return testPlan;
	}

	private String addProtocolIfMissing(String url) {
		if (url.startsWith("http")) {
			return url;
		} else {
			return "http://" + url;
		}
	}

	private void startJMeter(Path testPlanPath) throws IOException {
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(jmeterHome + "/bin/jmeter -t " + testPlanPath.toString());
		pr.getInputStream();
	}

}
