package org.continuity.benchflow.amqp;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.IdpaAnnotation;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.benchflow.artifact.ContinuITyModel;
import org.continuity.benchflow.config.RabbitMqConfig;
import org.continuity.benchflow.transform.ModelTransformater;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import cloud.benchflow.dsl.definition.workload.HttpWorkload;

/**
 * 
 * @author Manuel Palenga
 *
 */
@Component
public class BenchFlowDSLCreationAmqpHandler {


	private static final Logger LOGGER = LoggerFactory.getLogger(BenchFlowDSLCreationAmqpHandler.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private MemoryStorage<HttpWorkload> storage;
	
	/**
	 * Transforms a workload model into a BenchFlow DSL. 
	 * 
	 * @param task
	 * 				Task to be processed.
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void createDSL(TaskDescription task) {
		
		TaskReport report;

		String workloadModelLink = task.getSource().getWorkloadModelLinks().getLink();

		if (workloadModelLink == null) {
			LOGGER.error("Task {}: Cannot create a load test. The workload link is null!", task.getTaskId());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			LOGGER.info("Task {}: Creating a load test from {}...", task.getTaskId(), workloadModelLink);

			ContinuITyModel continuITyModel = getContinuITyModel(workloadModelLink, task.getTag());
			
			if (continuITyModel == null) {
				LOGGER.error("The workload model at {} does not provide all required input fields!", workloadModelLink);
				report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
			} else {
				ModelTransformater modelConverter = new ModelTransformater();
				HttpWorkload benchflowWorkload = modelConverter.transformToBenchFlow(continuITyModel);

				String id = storage.put(benchflowWorkload, task.getTag());
				LOGGER.info("Task {}: Created a load test from {}.", task.getTaskId(), workloadModelLink);

				report = TaskReport.successful(task.getTaskId(),
						new LinkExchangeModel().getLoadTestLinks().setType(LoadTestType.BENCHFLOW).setLink(RestApi.BenchFlow.DSL.GET.requestUrl(id).withoutProtocol().get()).parent());
			}
		}

		
		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

	private ContinuITyModel getContinuITyModel(String workloadModelLink, String tag) {
		
		LinkExchangeModel workloadLinks = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadModelLink), LinkExchangeModel.class);
		
		if (workloadLinks == null) {
			LOGGER.error("Workload links with tag {} is null! Aborting.", tag);
			return null;
		}
		
		if (workloadLinks.getWorkloadModelLinks() == null || workloadLinks.getWorkloadModelLinks().getBehaviorLink() == null) {
			LOGGER.error("Behavior links with tag {} is null! Aborting.", tag);
			return null;
		}

		BehaviorModel behaviorModel = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadLinks.getWorkloadModelLinks().getBehaviorLink()), BehaviorModel.class);
		
		ApplicationAnnotation annotation;
		try {
			annotation = restTemplate.getForObject(IdpaAnnotation.Annotation.GET.requestUrl(tag).get(), ApplicationAnnotation.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Received a non-200 response: {} ({}) - {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase(), e.getResponseBodyAsString());
			return null;
		}

		if (annotation == null) {
			LOGGER.error("Annotation with tag {} is null! Aborting.", tag);
			return null;
		}

		Application application = restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);

		if (application == null) {
			LOGGER.error("Application with tag {} is null! Aborting.", tag);
			return null;
		}
		
		return new ContinuITyModel(behaviorModel, application, annotation);
	}
	
}
