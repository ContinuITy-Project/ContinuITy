package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.Intensity.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.Intensity.Paths.UPLOAD;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class IntensityController {

	private static final Logger LOGGER = LoggerFactory.getLogger(IntensityController.class);

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private ElasticsearchIntensityManager elasticManager;

	@RequestMapping(value = UPLOAD, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "tailoring", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> upload(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("tailoring") String tailoringStr, @PathVariable String group,
			@RequestBody Map<String, Long> intensities) throws IOException {

		Map<Date, Long> intensitiesPerDate;
		try {
			intensitiesPerDate = ApiFormats.parseKeys(intensities);
		} catch (ParseException e) {
			LOGGER.error("Cannot parse date keys!", e);
			return ResponseEntity.badRequest().body("Illegal date format!");
		}

		List<String> tailoring = Session.convertStringToTailoring(tailoringStr);
		List<IntensityRecord> records = toIntensityRecords(aid, group, intensitiesPerDate);
		elasticManager.storeOrUpdateIntensities(aid, tailoring, records);

		return ResponseEntity.ok("Updated the intensities!");
	}

	private List<IntensityRecord> toIntensityRecords(AppId aid, String group, Map<Date, Long> intensities) {
		Duration resolution = configProvider.getConfiguration(aid).getIntensity().getResolution();
		long resolutionMillis = (resolution.getSeconds() * 1000) + (resolution.getNano() / 1000000);

		Map<Long, Double> groups = intensities.entrySet().stream().map(e -> Pair.of((e.getKey().getTime() / resolutionMillis) * resolutionMillis, e.getValue()))
				.collect(Collectors.groupingBy(Pair::getLeft, Collectors.averagingLong(Pair::getRight)));

		return groups.entrySet().stream().map(i -> toIntensityRecord(group, i)).collect(Collectors.toList());
	}

	private IntensityRecord toIntensityRecord(String group, Entry<Long, Double> intensityEntry) {
		IntensityRecord record = new IntensityRecord();
		record.setTimestamp(intensityEntry.getKey());
		record.setIntensity(Collections.singletonMap(group, Math.round(intensityEntry.getValue())));

		return record;
	}

}
