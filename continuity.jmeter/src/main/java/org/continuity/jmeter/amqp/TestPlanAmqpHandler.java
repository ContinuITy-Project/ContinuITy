package org.continuity.jmeter.amqp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.controllers.TestPlanController;
import org.continuity.jmeter.entities.LoadTestSpecification;
import org.continuity.jmeter.entities.TestPlanBundle;
import org.continuity.jmeter.io.TestPlanWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	/**
	 * Listens to the {@link RabbitMqConfig#EXECUTE_LOAD_TEST_QUEUE_NAME} queue, annotates the test
	 * plan and executes the test.
	 *
	 * @param specification
	 *            The specification of the test plan.
	 */
	@RabbitListener(queues = RabbitMqConfig.EXECUTE_LOAD_TEST_QUEUE_NAME)
	public void executeTestPlan(LoadTestSpecification specification) {
		LOGGER.debug("Received test plan specification.");

		TestPlanBundle testPlanPack = testPlanController.createAndGetLoadTest(specification.getWorkloadModelType(), specification.getWorkloadModelId(), specification.getTag());

		LOGGER.debug("Got an annotated test plan pack.");

		Path tmpPath;

		try {
			tmpPath = Files.createTempDirectory("jmeter-test-plan");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Path testPlanPath = testPlanWriter.write(testPlanPack.getTestPlan(), testPlanPack.getBehaviors(), tmpPath);
		LOGGER.info("Created a test plan at {}.", testPlanPath);

		// TODO: Run the test:
		// StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
		// jmeterEngine.configure(annotatedTestPlan);
		// jmeterEngine.run();
	}

}
