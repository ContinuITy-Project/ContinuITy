package org.continuity.jmeter.amqp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.entities.TestPlanPack;
import org.continuity.jmeter.io.JMeterProcess;
import org.continuity.jmeter.io.TestPlanWriter;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(IncomingTestPlanAmqpHandler.class);

	@Autowired
	private TestPlanWriter testPlanWriter;

	@Autowired
	private JMeterProcess jmeterProcess;

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Listens to the {@link RabbitMqConfig#LOAD_TEST_CREATED_QUEUE_NAME} queue, annotates the test
	 * plan and executes the test.
	 *
	 * @param testPlanPack
	 *            The bundle holding the test plan.
	 */
	@RabbitListener(queues = RabbitMqConfig.LOAD_TEST_CREATED_QUEUE_NAME)
	public void handleIncomingTestPlan(TestPlanPack testPlanPack) {
		LOGGER.info("Received test plan.");

		ListedHashTree annotatedTestPlan = createAnnotatedTestPlan(testPlanPack);

		if (annotatedTestPlan == null) {
			LOGGER.error("Could not annotate the test plan! Ignoring the annotation.");
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
		jmeterProcess.start(testPlanPath);

		// Run the test:
		// StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
		// jmeterEngine.configure(annotatedTestPlan);
		// jmeterEngine.run();
	}

	private ListedHashTree createAnnotatedTestPlan(TestPlanPack testPlanPack) {
		SystemAnnotation annotation;
		try {
			annotation = restTemplate.getForObject(getAnnotationLink(testPlanPack.getTag(), "annotation"), SystemAnnotation.class);
		} catch (RestClientException e) {
			e.printStackTrace();
			return null;
		}

		if (annotation == null) {
			LOGGER.error("Annotation with tag {} is null! Aborting.", testPlanPack.getTag());
			return null;
		}

		SystemModel systemModel = restTemplate.getForObject(getAnnotationLink(testPlanPack.getTag(), "system"), SystemModel.class);

		if (systemModel == null) {
			LOGGER.error("System with tag {} is null! Aborting.", testPlanPack.getTag());
			return null;
		}

		ListedHashTree testPlan = testPlanPack.getTestPlan();
		JMeterAnnotator annotator = new JMeterAnnotator(testPlan, systemModel);
		annotator.addAnnotations(annotation);

		return testPlan;
	}

	private String getAnnotationLink(String tag, String suffix) {
		return "http://workload-annotation/ann/" + tag + "/" + suffix;
	}

}
