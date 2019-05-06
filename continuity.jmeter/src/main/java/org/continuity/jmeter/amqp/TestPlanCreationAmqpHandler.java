package org.continuity.jmeter.amqp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.entities.config.PropertySpecification;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.jmeter.JMeterPropertiesCorrector;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TestPlanCreationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanCreationAmqpHandler.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private MixedStorage<JMeterTestPlanBundle> storage;

	private JMeterPropertiesCorrector jmeterPropertiesCorrector = new JMeterPropertiesCorrector();

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void createTestPlan(TaskDescription task) {
		TaskReport report;

		String workloadModelLink = task.getSource().getWorkloadModelLinks().getLink();

		if (workloadModelLink == null) {
			LOGGER.error("Task {}: Cannot create a load test. The workload link is null!", task.getTaskId());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			LOGGER.info("Task {}: Creating a load test from {}...", task.getTaskId(), workloadModelLink);

			LinkExchangeModel workloadLinks = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadModelLink), LinkExchangeModel.class);

			if ((workloadLinks == null) || (workloadLinks.getWorkloadModelLinks().getJmeterLink() == null)) {
				LOGGER.error("The workload model at {} does not provide a link to JMeter!", workloadModelLink);
				report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
			} else {
				JMeterTestPlanBundle bundle = createAndGetLoadTest(workloadLinks, task.getTag(), task.getProperties(), task.getModularizationOptions());

				String id = storage.put(bundle, task.getTag(), task.isLongTermUse());
				LOGGER.info("Task {}: Created a load test from {}.", task.getTaskId(), workloadModelLink);

				report = TaskReport.successful(task.getTaskId(),
						new LinkExchangeModel().getLoadTestLinks().setType(LoadTestType.JMETER).setLink(RestApi.JMeter.TestPlan.GET.requestUrl(id).withoutProtocol().get()).parent());
			}
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

	/**
	 * Transforms a workload model into a JMeter test and returns it. The workload model is
	 * specified by a link, i.e., {@code TYPE/model/ID}.
	 *
	 * @param workloadLinks
	 *            The links pointing to the workload model.
	 * @param tag
	 *            The tag to be used to retrieve the annotation.
	 * @return The transformed JMeter test plan.
	 */
	private JMeterTestPlanBundle createAndGetLoadTest(LinkExchangeModel workloadLinks, String tag, PropertySpecification properties, ModularizationOptions modularizationOptions) {
		JMeterTestPlanBundle testPlanPack = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadLinks.getWorkloadModelLinks().getJmeterLink()), JMeterTestPlanBundle.class);

		ListedHashTree annotatedTestPlan = testPlanPack.getTestPlan();
		JMeterAnnotator annotator = new JMeterAnnotator(annotatedTestPlan, restTemplate);

		// Check if modularization is enabled in the order.
		if (null != modularizationOptions) {
			List<String> serviceTags = new ArrayList<String>(modularizationOptions.getServices().keySet());
			annotatedTestPlan = annotator.createAnnotatedTestPlan(serviceTags);
		} else {
			annotatedTestPlan = annotator.createAnnotatedTestPlan(Arrays.asList(tag));
		}

		if (properties == null) {
			LOGGER.warn("Could not set JMeter properties, as they are null.");
		} else {
			if (properties.getNumUsers() != null) {
				jmeterPropertiesCorrector.setNumberOfUsers(testPlanPack.getTestPlan(), properties.getNumUsers());
				LOGGER.info("Set number of users to {}.", properties.getNumUsers());
			}
			if (properties.getDuration() != null) {
				jmeterPropertiesCorrector.setDuration(testPlanPack.getTestPlan(), properties.getDuration());
				LOGGER.info("Set duration to {}.", properties.getDuration());
			}
			if (properties.getRampup() != null) {
				jmeterPropertiesCorrector.setRampup(testPlanPack.getTestPlan(), properties.getRampup());
				LOGGER.info("Set rampup to {}.", properties.getRampup());
			}
		}

		jmeterPropertiesCorrector.autocorrect(testPlanPack.getTestPlan());

		return new JMeterTestPlanBundle(annotatedTestPlan, testPlanPack.getBehaviors());
	}

}
