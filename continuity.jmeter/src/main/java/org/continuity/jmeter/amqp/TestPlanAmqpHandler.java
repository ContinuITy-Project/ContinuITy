package org.continuity.jmeter.amqp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.testelement.TestStateListener;
import org.continuity.commons.jmeter.JMeterPropertiesCorrector;
import org.continuity.commons.jmeter.TestPlanWriter;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.controllers.TestPlanController;
import org.continuity.jmeter.entities.LoadTestSpecification;
import org.continuity.jmeter.entities.TestPlanBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class TestPlanAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanAmqpHandler.class);

	@Autowired
	private TestPlanController testPlanController;

	@Autowired
	private TestPlanWriter testPlanWriter;

	@Autowired
	private AmqpTemplate amqpTemplate;

	private JMeterPropertiesCorrector behaviorPathsCorrector = new JMeterPropertiesCorrector();

	/**
	 * Listens to the {@link RabbitMqConfig#CREATE_AND_EXECUTE_LOAD_TEST_QUEUE_NAME} queue,
	 * annotates the test plan and executes the test.
	 *
	 * @param specification
	 *            The specification of the test plan.
	 */
	@RabbitListener(queues = RabbitMqConfig.CREATE_AND_EXECUTE_LOAD_TEST_QUEUE_NAME)
	public void createAndExecuteTestPlan(LoadTestSpecification specification) {
		LOGGER.debug("Received test plan specification.");

		TestPlanBundle testPlanBundle = testPlanController.createAndGetLoadTest(specification.getWorkloadModelType(), specification.getWorkloadModelId(), specification.getTag());

		LOGGER.debug("Got an annotated test plan pack.");

		executeTestPlan(testPlanBundle);
	}

	/**
	 * Listens to the {@link RabbitMqConfig#EXECUTE_LOAD_TEST_QUEUE_NAME} queue and executes the
	 * sent JMeter test plan.
	 *
	 * @param testPlanBundle
	 *            Test plan bundle including the test plan itself and the behaviors for
	 *            Markov4JMeter.
	 */
	@RabbitListener(queues = RabbitMqConfig.EXECUTE_LOAD_TEST_QUEUE_NAME)
	public void executeTestPlan(TestPlanBundle testPlanBundle) {
		Path tmpPath;

		try {
			tmpPath = Files.createTempDirectory("jmeter-test-plan");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Path resultsPath = tmpPath.resolve("results.csv");

		behaviorPathsCorrector.correctPaths(testPlanBundle.getTestPlan(), tmpPath);
		behaviorPathsCorrector.configureResultFile(testPlanBundle.getTestPlan(), resultsPath);
		behaviorPathsCorrector.prepareForHeadlessExecution(testPlanBundle.getTestPlan());
		Path testPlanPath = testPlanWriter.write(testPlanBundle.getTestPlan(), testPlanBundle.getBehaviors(), tmpPath);
		LOGGER.info("Created a test plan at {}.", testPlanPath);

		JMeter jmeter = new JMeter();
		String[] arguments = { "-n", "-t", testPlanPath.toAbsolutePath().toString() };

		StandardJMeterEngine.register(new TestStateListener() {

			@Override
			public void testStarted(String arg0) {
				// do nothing
			}

			@Override
			public void testStarted() {
				// do nothing
			}

			@Override
			public void testEnded(String arg0) {
				testEnded();
			}

			@Override
			public void testEnded() {
				try {
					amqpTemplate.convertAndSend(RabbitMqConfig.PROVIDE_REPORT_EXCHANGE_NAME, "jmeter", FileUtils.readFileToString(resultsPath.toFile(), Charset.defaultCharset()));
					LOGGER.info("JMeter test finished. Results are stored to {}.", resultsPath);
				} catch (AmqpException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		jmeter.start(arguments);
	}

}
