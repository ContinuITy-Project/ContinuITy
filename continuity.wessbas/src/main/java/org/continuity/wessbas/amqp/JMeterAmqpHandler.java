package org.continuity.wessbas.amqp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.entities.JMeterTestPlanPack;
import org.continuity.wessbas.entities.LoadTestSpecification;
import org.continuity.wessbas.entities.WorkloadModelStorageEntry;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.continuity.wessbas.transform.jmeter.WessbasToJmeterConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
@Component
public class JMeterAmqpHandler {

	private static final String UNKNOWN_ID = "UNKNOWN";

	private static final Pattern WORKLOAD_LINK_PATTERN = Pattern.compile("^\\w+/wessbas/(.*)/workload$");

	@Autowired
	private WessbasToJmeterConverter jmeterConverter;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RabbitListener(queues = RabbitMqConfig.LOAD_TEST_NEEDED_QUEUE_NAME)
	public void createTestPlan(LoadTestSpecification specification) {
		String id = extractIdFromLink(specification.getWorkloadModelLink());

		if (id == null) {
			// TODO: print error message
			return;
		}

		if (id == UNKNOWN_ID) {
			// TODO: log error message
		} else {
			WorkloadModelStorageEntry workloadModelEntry = SimpleModelStorage.instance().get(id);

			if (workloadModelEntry == null) {
				// TODO: Print error message
				System.err.println("There is no workload model with id " + id);
				return;
			}

			WorkloadModel workloadModel = workloadModelEntry.getWorkloadModel();

			JMeterTestPlanPack testPlanPack = jmeterConverter.convertToLoadTest(workloadModel);
			testPlanPack.setAnnotationLink(specification.getAnnotationLink());

			amqpTemplate.convertAndSend(RabbitMqConfig.LOAD_TEST_CREATED_EXCHANGE_NAME, "wessbas.jmeter", testPlanPack);
		}
	}

	private String extractIdFromLink(String workloadModelLink) {
		Matcher matcher = WORKLOAD_LINK_PATTERN.matcher(workloadModelLink);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

}
