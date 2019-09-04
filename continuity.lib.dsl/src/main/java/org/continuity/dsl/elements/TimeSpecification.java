package org.continuity.dsl.elements;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.dsl.elements.timeframe.ConditionalTimespec;
import org.continuity.dsl.elements.timeframe.ExtendingTimespec;
import org.continuity.dsl.elements.timeframe.Timerange;
import org.continuity.dsl.timeseries.IntensityRecord;
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
	 * @return {@code true} if the specification applies to the passed record.
	 */
	default boolean appliesTo(IntensityRecord record) {
		boolean appliesToDate = appliesToDate(DateUtils.fromEpochMillis(record.getTimestamp()));

		boolean contextIsNull = record.getContext() == null;
		boolean appliesToBoolean = contextIsNull || (record.getContext().getBoolean() == null) || appliesToBoolean(record.getContext().getBoolean());
		boolean appliesToNumeric = contextIsNull || (record.getContext().getNumeric() == null)
				|| record.getContext().getNumeric().stream().map(n -> appliesToNumerical(n.getName(), n.getValue())).reduce(Boolean::logicalAnd).orElse(true);
		boolean appliesToString = contextIsNull || (record.getContext().getString() == null)
				|| record.getContext().getString().stream().map(s -> appliesToString(s.getName(), s.getValue())).reduce(Boolean::logicalAnd).orElse(true);

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
	 * Returns whether the time specification applies to a given value of a numerical variable.
	 *
	 * @param variable
	 *            The variable to check.
	 * @param value
	 *            The value of the variable.
	 * @return {@code true} if the passed value fits to the specification.
	 */
	boolean appliesToNumerical(String variable, double value);

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
	 * Returns whether the time specification applies to a given value of a string variable.
	 *
	 * @param variable
	 *            The variable to check.
	 * @param value
	 *            The value of the variable.
	 * @return {@code true} if the passed value fits to the specification.
	 */
	boolean appliesToString(String variable, String value);

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
	 * @return The queries as pair of the query it self and a boolean indicating whether the query
	 *         returned by {@link #toElasticQuery()} should be treated positively ({@code true};
	 *         {@code must}) or negatively ({@code false}; {@code must_not}).
	 */
	List<Pair<QueryBuilder, Boolean>> toElasticQuery();

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

}
