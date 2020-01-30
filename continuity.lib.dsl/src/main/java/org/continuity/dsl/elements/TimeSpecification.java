package org.continuity.dsl.elements;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.dsl.elements.timeframe.ConditionalTimespec;
import org.continuity.dsl.elements.timeframe.ExtendingTimespec;
import org.continuity.dsl.elements.timeframe.Timerange;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.dsl.timeseries.NumericVariable;
import org.continuity.dsl.timeseries.StringVariable;
import org.continuity.dsl.utils.DateUtils;
import org.elasticsearch.index.query.QueryBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Specifies the time range, for which a load test is to be generated.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
@JsonSubTypes({ @Type(value = Timerange.class, name = "timerange"), @Type(value = ConditionalTimespec.class, name = "conditional"), @Type(value = ExtendingTimespec.class, name = "extended") })
public interface TimeSpecification {

	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss-SSSX";

	@JsonIgnore
	default void setDefaultMinDate(LocalDateTime min) {
	}

	@JsonIgnore
	default void setDefaultMaxDate(LocalDateTime max) {
	}

	/**
	 * Returns whether the time specification applies to a given intensity record.
	 *
	 * @param record
	 *            The intensity record (potentially including a context)
	 * @param timeZone
	 *            The time zone in which the timestamps should be evaluated.
	 * @return {@code true} if the specification applies to the passed record.
	 */
	default boolean appliesTo(IntensityRecord record, ZoneId timeZone) {
		boolean appliesToDate = appliesToDate(DateUtils.fromEpochMillis(record.getTimestamp(), timeZone));

		boolean contextIsNull = record.getContext() == null;
		boolean appliesToBoolean = appliesToBoolean(contextIsNull ? null : record.getContext().getBoolean());

		boolean appliesToNumeric = appliesToNumerical(contextIsNull ? null : record.getContext().getNumericVariables());
		boolean appliesToString = appliesToString(contextIsNull ? null : record.getContext().getStringVariables());

		return appliesToDate && appliesToBoolean && appliesToNumeric && appliesToString;
	}

	/**
	 * Returns whether the time specification applies to a given date.
	 *
	 * @param date
	 *            The date to check.
	 * @return {@code true} it the passed date is within the time specification.
	 */
	boolean appliesToDate(LocalDateTime date);

	/**
	 * Returns whether the time specification applies to the given numerical variables.
	 *
	 * @param variables
	 *            The variables to check.
	 * @return {@code true} if the passed value fits to the specification.
	 */
	boolean appliesToNumerical(Set<NumericVariable> variables);

	/**
	 * Returns whether the time specification applies to a given list of boolean variables that
	 * occur.
	 *
	 * @param occurring
	 *            The boolean variables to check.
	 * @return {@code true} if the passed variables fit to the specification.
	 */
	boolean appliesToBoolean(Set<String> occurring);

	/**
	 * Returns whether the time specification applies to the given string variables.
	 *
	 * @param variable
	 *            The variables to check.
	 * @return {@code true} if the passed value fits to the specification.
	 */
	boolean appliesToString(Set<StringVariable> variables);

	/**
	 * Returns whether postprocessing is required to retrieve all intensities.
	 *
	 * @return {@code true} if postprocessing should be done.
	 */
	default boolean hasPostprocessing() {
		return false;
	}

	/**
	 * Processes the list of dates to which any of the time specifications applied.
	 *
	 * @param applied
	 *            List of applied dates.
	 * @param step
	 *            The step width.
	 * @return List of finally applied dates.
	 */
	default List<LocalDateTime> postprocess(List<LocalDateTime> applied, Duration step) {
		return applied;
	}

	/**
	 * Transforms the time specification to elasticsearch queries.
	 *
	 * @param timeZone
	 *            The time zone.
	 *
	 * @return The queries as pair of the query it self and a boolean indicating whether the query
	 *         returned by {@link #toElasticQuery()} should be treated positively ({@code true};
	 *         {@code must}) or negatively ({@code false}; {@code must_not}).
	 */
	List<Pair<QueryBuilder, Boolean>> toElasticQuery(ZoneId timeZone);

	/**
	 * Returns whether this specification requires postprocessing.
	 *
	 * @return {@code true} if postprocessing is required.
	 */
	default boolean requiresPostprocessing() {
		return false;
	}

	/**
	 * Transforms the time specification to queries that fetches missing dates from the database.
	 *
	 * @param applied
	 *            The dates fetched from the database.
	 * @param step
	 *            The step width.
	 * @return The queries.
	 */
	default Optional<QueryBuilder> toPostprocessElasticQuery(List<LocalDateTime> applied, Duration step) {
		return Optional.empty();
	}

	@JsonIgnore
	default Optional<LocalDateTime> getMaxDate() {
		return Optional.empty();
	}

	@JsonIgnore
	default Optional<Duration> getMaxEndAddition() {
		return Optional.empty();
	}

	@JsonIgnore
	default Optional<LocalDateTime> getMinDate() {
		return Optional.empty();
	}

	@JsonIgnore
	default Optional<Duration> getMaxBeginningAddition() {
		return Optional.empty();
	}

	@JsonIgnore
	default Set<String> getReferredContextVariables() {
		return Collections.emptySet();
	}

}
