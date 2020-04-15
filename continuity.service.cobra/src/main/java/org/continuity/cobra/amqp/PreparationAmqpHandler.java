package org.continuity.cobra.amqp;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.BehaviorModelType;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.entities.ForecastTimerange;
import org.continuity.cobra.entities.ForecasticInput;
import org.continuity.cobra.entities.ForecasticResult;
import org.continuity.cobra.entities.TimedContextRecord;
import org.continuity.cobra.entities.TypeAndProperties;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.lctl.WorkloadDescription;
import org.continuity.lctl.elements.TimeSpecification;
import org.continuity.lctl.schema.IgnoreByDefaultValue;
import org.continuity.lctl.timeseries.IntensityRecord;
import org.continuity.lctl.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class PreparationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreparationAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ElasticsearchSessionManager elasticSessionManager;

	@Autowired
	private ElasticsearchTraceManager elasticTraceManager;

	@Autowired
	private ElasticsearchIntensityManager elasticIntensityManager;

	@Autowired
	private ElasticsearchBehaviorManager elasticBehaviorManager;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private MixedStorage<List<ForecastIntensityRecord>> intensityStorage;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void prepareInitialData(TaskDescription task) throws IOException, TimeoutException {
		WorkloadDescription description = task.getWorkloadDescription();

		LOGGER.info("Processing task {}...", task.getTaskId());

		if (description == null) {
			sendReport(TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE));
			return;
		}

		CobraConfiguration config = configProvider.getConfiguration(task.getAppId());
		Optional<Long> perspective = Optional.ofNullable(task.getPerspective()).map(p -> {
			LOGGER.info("Task {}: Considering {} to be 'now'.", task.getTaskId(), p);
			description.setNow(p);
			return p.atZone(config.getTimeZone()).toInstant().toEpochMilli();
		});

		List<String> tailoring = extractServices(task);

		List<IntensityRecord> intensities = readIntensities(task.getAppId(), tailoring, description);
		List<ForecastTimerange> ranges = extractRanges(task.getAppId(), intensities);

		LOGGER.info("Task {}: Get {} in ranges {}...", task.getTaskId(), task.getTarget().toPrettyString(), ranges);

		List<ForecastTimerange> restrictedRanges = perspective.map(p -> ranges.stream().filter(r -> r.getFrom() < p)
				.map(r -> new ForecastTimerange(r.getFrom(), Math.min(r.getTo(), p), config.getTimeZone()))
				.filter(r -> r.getTo() < r.getFrom()).collect(Collectors.toList())).orElse(ranges);

		TaskReport report;

		switch (task.getTarget()) {
		case TRACES:
			report = createTraceLink(task, restrictedRanges);
			break;
		case SESSIONS:
			report = createSessionLink(task, restrictedRanges);
			break;
		case BEHAVIOR_MODEL:
			report = createBehaviorLink(task, restrictedRanges, perspective);
			break;
		default:
			LOGGER.error("Task {}: Cannot generate {}!", task.getTaskId(), task.getTarget().toPrettyString());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
			break;
		}

		if (report.isSuccessful()) {
			report.getResult().setIntensity(doForecast(task, ranges, intensities, perspective));
		}

		sendReport(report);
	}

	private List<IntensityRecord> readIntensities(AppId aid, List<String> tailoring, WorkloadDescription description) throws IOException, TimeoutException {
		CobraConfiguration config = configProvider.getConfiguration(aid);
		Duration resolution = config.getIntensity().getResolution();
		ZoneId timeZone = config.getTimeZone();

		elasticIntensityManager.fillIntensities(aid, tailoring, description.getMinDate(), description.getMaxDate(), resolution, timeZone);

		List<IntensityRecord> intensities = elasticIntensityManager.readDescribedIntensities(aid, tailoring, description, timeZone);

		if (description.requiresPostprocessing()) {
			List<LocalDateTime> appliedDates = intensities.stream().map(IntensityRecord::getTimestamp).map(t -> Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDateTime())
					.collect(Collectors.toList());

			List<IntensityRecord> additional = elasticIntensityManager.readPostprocessing(aid, tailoring, description, appliedDates, resolution);

			intensities.addAll(additional);
			intensities.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
		}

		return intensities;
	}

	private List<ForecastTimerange> extractRanges(AppId aid, List<IntensityRecord> intensities) {
		CobraConfiguration config = configProvider.getConfiguration(aid);
		long resolution = config.getIntensity().getResolution().toMillis();

		List<ForecastTimerange> ranges = new ArrayList<>();
		IntensityRecord rangeStart = intensities.get(0);
		IntensityRecord last = null;

		for (IntensityRecord next : intensities) {
			LocalDateTime nextDate = DateUtils.fromEpochMillis(next.getTimestamp(), config.getTimeZone());
			LocalDateTime lastDate = last == null ? nextDate : DateUtils.fromEpochMillis(last.getTimestamp(), config.getTimeZone());
			long difference = ChronoUnit.SECONDS.between(lastDate, nextDate) * 1000;

			if ((last != null) && (difference > resolution)) {
				ranges.add(new ForecastTimerange(rangeStart.getTimestamp(), last.getTimestamp(), config.getTimeZone()));
				rangeStart = next;
			}

			last = next;
		}

		ranges.add(new ForecastTimerange(rangeStart.getTimestamp(), last.getTimestamp(), config.getTimeZone()));

		LOGGER.info("{}: Extracted ranges {}.", aid, ranges);

		return ranges;
	}

	private TaskReport createTraceLink(TaskDescription task, List<ForecastTimerange> ranges) throws IOException {
		long count = 0;

		if (ranges.isEmpty()) {
			count = elasticTraceManager.countTraces(task.getAppId(), null, null, null);
		} else {
			for (ForecastTimerange range : ranges) {
				count += elasticTraceManager.countTraces(task.getAppId(), null, new Date(range.getFrom()), new Date(range.getTo()));
			}
		}

		if (count == 0) {
			LOGGER.error("Task {}: There are no such traces available!", task.getTaskId());
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel();
			artifacts.getTraceLinks().setLink(formatTraceLink(task.getAppId(), task.getVersion(), ranges));
			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private TaskReport createSessionLink(TaskDescription task, List<ForecastTimerange> ranges) throws IOException {
		List<String> services = extractServices(task);

		long count = 0;

		if (ranges.isEmpty()) {
			count = elasticSessionManager.countSessionsOverlapping(task.getAppId(), null, services, null, null);
		} else {
			for (ForecastTimerange range : ranges) {
				// This might count sessions twice.
				// However, it is sufficient for testing whether sessions are present.
				count += elasticSessionManager.countSessionsOverlapping(task.getAppId(), null, services, new Date(range.getFrom()), new Date(range.getTo()));

				if (count > 0) {
					break;
				}
			}
		}

		if (count == 0) {
			LOGGER.error("Task {}: There are no such sessions available!", task.getTaskId());
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel();
			artifacts.getSessionLinks().setSimpleLink(formatSessionLink(RestApi.Cobra.Sessions.GET_SIMPLE, task.getAppId(), services, ranges))
					.setExtendedLink(formatSessionLink(RestApi.Cobra.Sessions.GET_EXTENDED, task.getAppId(), services, ranges));
			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private TaskReport createBehaviorLink(TaskDescription task, List<ForecastTimerange> ranges, Optional<Long> perspective) throws IOException, TimeoutException {
		AppId aid = task.getAppId();
		List<String> tailoring = extractServices(task);
		OptionalLong before = ranges.stream().mapToLong(ForecastTimerange::getTo).max();

		RequestBuilder reqBuilder = RestApi.Cobra.BehaviorModel.GET_LATEST.requestUrl(aid, Session.convertTailoringToString(tailoring));

		if (before.isPresent() || perspective.isPresent()) {
			reqBuilder.withQuery("before", Long.toString(before.orElse(perspective.orElse(0L))));
		}

		String link = reqBuilder.withoutProtocol().get();

		MarkovBehaviorModel behaviorModel;
		try {
			behaviorModel = (before.isPresent() || perspective.isPresent()) ? elasticBehaviorManager.readLatest(aid, tailoring, before.orElse(perspective.orElse(0L)))
					: elasticBehaviorManager.readLatest(aid, tailoring);
		} catch (HttpStatusCodeException e) {
			behaviorModel = null;
		}

		if (behaviorModel == null) {
			LOGGER.error("Task {}: There is no such behavior model available: {}", task.getTaskId(), link);
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel().getBehaviorModelLinks().setLink(link).setType(BehaviorModelType.MARKOV_CHAIN).parent();

			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private String formatSessionLink(RestEndpoint endpoint, AppId aid, List<String> services, List<ForecastTimerange> ranges) {
		RequestBuilder reqBuilder = endpoint.requestUrl(aid.dropService(), Session.convertTailoringToString(services));

		for (ForecastTimerange range : ranges) {
			reqBuilder.withQuery("from", ApiFormats.formatMillisAsDate(range.getFrom())).withQuery("to", ApiFormats.formatMillisAsDate(range.getTo()));
		}

		return reqBuilder.withoutProtocol().get();
	}

	private String formatTraceLink(AppId aid, VersionOrTimestamp version, List<ForecastTimerange> ranges) {
		RequestBuilder reqBuilder;

		if (version == null) {
			reqBuilder = RestApi.Cobra.MeasurementData.GET.requestUrl(aid);
		} else {
			reqBuilder = RestApi.Cobra.MeasurementData.GET_VERSION.requestUrl(aid, version);
		}

		for (ForecastTimerange range : ranges) {
			reqBuilder.withQuery("from", ApiFormats.formatMillisAsDate(range.getFrom())).withQuery("to", ApiFormats.formatMillisAsDate(range.getTo()));
		}

		return reqBuilder.withoutProtocol().get();
	}

	private String doForecast(TaskDescription task, List<ForecastTimerange> ranges, List<IntensityRecord> intensities, Optional<Long> perspective) {
		LOGGER.info("Task {}: Doing forecast...", task.getTaskId());

		CobraConfiguration config = configProvider.getConfiguration(task.getAppId());
		WorkloadDescription description = task.getWorkloadDescription();

		ForecasticInput input = new ForecasticInput().setAppId(task.getAppId()).setTailoring(extractServices(task))
				.setApproach(task.getOptionsOrDefault().getForecastOrDefault().getApproachOrDefault());

		if (task.getPerspective() != null) {
			input.setPerspective(task.getPerspective().atZone(config.getTimeZone()).toInstant().toEpochMilli());
		}

		input.setForecastTotal(task.getOptionsOrDefault().getForecastOrDefault().getTotal().orElse(false));

		input.setRanges(ranges).setResolution(config.getIntensity().getResolution().toMillis());

		IgnoreByDefaultValue ignore = config.getContext().ignoreByDefault();
		Set<String> contextVariables = config.getContext().getVariables().entrySet().stream().filter(e -> !ignore.ignore(e.getValue().getIgnoreByDefault())).map(Entry::getKey)
				.collect(Collectors.toSet());
		description.getTimeframe().stream().map(TimeSpecification::getReferredContextVariables).flatMap(Set::stream).forEach(contextVariables::add);
		input.setContext(prepareFutureContext(intensities, perspective, config, description));
		input.setContextVariables(contextVariables);

		input.setAggregation(TypeAndProperties.fromTypedProperties(description.getAggregation()));

		input.setAdjustments(
				Optional.ofNullable(description.getAdjustments()).map(l -> l.stream().map(TypeAndProperties::fromTypedProperties).collect(Collectors.toList())).orElse(Collections.emptyList()));

		ForecasticResult result = restTemplate.postForObject(RestApi.Forecastic.FORECAST.requestUrl().get(), input, ForecasticResult.class);
		LOGGER.info("Task {}: Received {} intensity records from forecastic.", task.getTaskId(), result.getIntensities().size());

		String id = intensityStorage.put(result.getIntensities(), task.getAppId(), task.isLongTermUse());
		LOGGER.info("Task {}: Stored intensity records to storage with ID {}.", task.getTaskId(), id);

		return RestApi.Cobra.Intensity.GET_FOR_ID.requestUrl(id).withoutProtocol().get();
	}

	private List<TimedContextRecord> prepareFutureContext(List<IntensityRecord> intensities, Optional<Long> perspective, CobraConfiguration config, WorkloadDescription description) {
		long maxIntensityTimestamp = intensities.stream().filter(i -> (i.getIntensity() != null) && !i.getIntensity().isEmpty()).mapToLong(IntensityRecord::getTimestamp).max().orElse(0);
		long effectivePerspective = perspective.map(p -> Math.min(p, maxIntensityTimestamp)).orElse(maxIntensityTimestamp);

		List<IntensityRecord> futureRecords = intensities.stream().filter(i -> i.getTimestamp() > effectivePerspective).collect(Collectors.toList());

		Set<String> timeframeVariables = description.getTimeframe().stream().map(TimeSpecification::getReferredContextVariables).flatMap(Set::stream).collect(Collectors.toSet());

		for (IntensityRecord record : futureRecords) {
			if (record.getContext() != null) {
				if (record.getContext().getNumeric() != null) {
					record.getContext().getNumeric().keySet().removeIf(ignoreVariable(timeframeVariables, config));

					if (record.getContext().getNumeric().isEmpty()) {
						record.getContext().setNumeric(null);
					}
				}

				if (record.getContext().getString() != null) {
					record.getContext().getString().keySet().removeIf(ignoreVariable(timeframeVariables, config));

					if (record.getContext().getString().isEmpty()) {
						record.getContext().setString(null);
					}
				}

				if (record.getContext().getBoolean() != null) {
					record.getContext().getBoolean().removeIf(ignoreVariable(timeframeVariables, config));

					if (record.getContext().getBoolean().isEmpty()) {
						record.getContext().setBoolean(null);
					}
				}
			}
		}

		description.adjustContext(futureRecords, config.getTimeZone());

		return futureRecords.stream().map(TimedContextRecord::fromIntensity).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private Predicate<String> ignoreVariable(Set<String> variables, CobraConfiguration config) {
		IgnoreByDefaultValue ignore = config.getContext().ignoreByDefault();
		return v -> !variables.contains(v) && ignore.ignore(config.getContext().getVariables().get(v).getIgnoreByDefault());
	}

	private void sendReport(TaskReport report) {
		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);

		if (report.isSuccessful()) {
			LOGGER.info("Finished task {} successfully.", report.getTaskId());
		} else {
			LOGGER.warn("Finished task {} with errors.", report.getTaskId());
		}
	}

	private List<String> extractServices(TaskDescription task) {
		List<ServiceSpecification> services = task.getEffectiveServices();

		if ((task.getOptions() != null) && (task.getOptions().getServiceTailoringOrDefault() == TailoringApproach.LOG_BASED) && TailoringUtils.doTailoring(services)) {
			return services.stream().map(ServiceSpecification::getService).collect(Collectors.toList());
		} else {
			return Collections.singletonList(AppId.SERVICE_ALL);
		}
	}

}
