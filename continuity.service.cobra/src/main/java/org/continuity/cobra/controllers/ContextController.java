package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.Context.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.Context.Paths.PUSH;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.api.exception.ServiceConfigurationException;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.dsl.schema.ContextSchema;
import org.continuity.dsl.schema.VariableSchema;
import org.continuity.dsl.timeseries.ContextRecord;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.dsl.validation.ContextValidityReport;
import org.continuity.dsl.validation.NewVariableReport;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controls storing of contexts.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class ContextController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContextController.class);

	@Autowired
	private ElasticsearchIntensityManager elasticManager;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@RequestMapping(value = PUSH, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<ContextValidityReport> storeContexts(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestBody Map<String, ContextRecord> contextMap) throws IOException, TimeoutException {
		Map<Date, ContextRecord> contextPerDate;
		try {
			contextPerDate = ApiFormats.parseKeys(contextMap);
		} catch (ParseException e) {
			LOGGER.error("Cannot parse date keys!", e);
			return ResponseEntity.badRequest().build();
		}

		CobraConfiguration config = configProvider.getConfiguration(aid);
		ContextSchema schema = config.getContext();

		ContextValidityReport report = contextMap.values().stream().map(schema::validate).reduce(ContextValidityReport::merge).orElse(new ContextValidityReport());

		if (report.isErroenous()) {
			LOGGER.error("Uploaded context for app-id {} is erroenous:\n{}", aid, report);
			return ResponseEntity.badRequest().body(report);
		}

		if (report.hasNew()) {
			LOGGER.info("Uploaded context for app-id {} has new variables:\n{}", aid, report);
			boolean successful = updateSchema(config, report.getNewlyAdded());

			if (!successful) {
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(report);
			}
		}

		storeContextsToDb(aid, contextPerDate, config);

		return ResponseEntity.ok(report);
	}

	/**
	 *
	 * @param config
	 * @param newlyAdded
	 * @return Whether the update was successful ({@code true}) or not ({@code false}).
	 */
	private boolean updateSchema(CobraConfiguration config, Set<NewVariableReport> newlyAdded) {
		ContextSchema schema = config.getContext();
		boolean ignoreNew = schema.ignoreByDefault().ignoreNew();

		synchronized (schema) {
			newlyAdded.stream().distinct().forEach(newVar -> {
				schema.getVariables().put(newVar.getName(), new VariableSchema(newVar.getType(), ignoreNew));
			});
		}

		try {
			configProvider.refresh(config);
		} catch (ServiceConfigurationException e) {
			LOGGER.error("Could not upload updated schema! Will rollback the changes and reject!", e);

			synchronized (schema) {
				Map<String, VariableSchema> vars = schema.getVariables();
				newlyAdded.stream().distinct().map(NewVariableReport::getName).forEach(vars::remove);
			}

			return false;
		}

		return true;
	}

	private void storeContextsToDb(AppId aid, Map<Date, ContextRecord> contextMap, CobraConfiguration config) throws IOException, TimeoutException {
		Duration resolution = configProvider.getConfiguration(aid).getIntensity().getResolution();
		long resolutionMillis = (resolution.getSeconds() * 1000) + (resolution.getNano() / 1000000);

		for (List<String> tailoring : config.getTailoring()) {
			elasticManager.storeOrUpdateIntensities(aid, tailoring, toIntensityRecords(contextMap, resolutionMillis));
		}
	}

	private Collection<IntensityRecord> toIntensityRecords(Map<Date, ContextRecord> contextMap, long resolutionMillis) {
		Map<Long, IntensityRecord> intensityPerDate = new HashMap<>();

		for (Entry<Date, ContextRecord> entry : contextMap.entrySet()) {
			long timestamp = (entry.getKey().getTime() / resolutionMillis) * resolutionMillis;
			IntensityRecord record = intensityPerDate.get(timestamp);

			if (record == null) {
				record = new IntensityRecord();
				record.setTimestamp(timestamp);
				record.setContext(new ContextRecord());

				intensityPerDate.put(timestamp, record);
			}

			record.getContext().merge(entry.getValue());
		}

		return intensityPerDate.values();
	}

}
