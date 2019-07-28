package org.continuity.dsl.context;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.continuity.dsl.context.timespec.AbsentSpecification;
import org.continuity.dsl.context.timespec.AfterSpecification;
import org.continuity.dsl.context.timespec.BeforeSpecification;
import org.continuity.dsl.context.timespec.EqualSpecification;
import org.continuity.dsl.context.timespec.GreaterSpecification;
import org.continuity.dsl.context.timespec.LessSpecification;
import org.continuity.dsl.context.timespec.OccurringSpecification;
import org.continuity.dsl.context.timespec.PlusBeforeSpecification;
import org.continuity.dsl.context.timespec.PlusSpecification;
import org.continuity.dsl.context.timespec.UnequalSpecification;
import org.elasticsearch.index.query.QueryBuilder;

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
@JsonSubTypes({ @Type(value = AfterSpecification.class, name = "after"), @Type(value = BeforeSpecification.class, name = "before"), @Type(value = OccurringSpecification.class, name = "occurring"),
		@Type(value = AbsentSpecification.class, name = "absent"), @Type(value = GreaterSpecification.class, name = "greater"), @Type(value = LessSpecification.class, name = "less"),
		@Type(value = EqualSpecification.class, name = "equal"), @Type(value = UnequalSpecification.class, name = "unequal"), @Type(value = PlusSpecification.class, name = "plus"),
		@Type(value = PlusBeforeSpecification.class, name = "plus-before") })
public interface TimeSpecification {

	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss-SSSX";

	/**
	 * Returns whether the time specification applies to a given date.
	 *
	 * @param date
	 *            The date to check.
	 * @return {@code true} it the passed date is within the time specification.
	 */
	boolean appliesToDate(Date date);

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
	boolean appliesToBoolean(List<String> occurring);

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
	default List<Date> postprocess(List<Date> applied, Duration step) {
		return applied;
	}

	/**
	 * Transforms the time specification to an elasticsearch query.
	 *
	 * @return The query.
	 */
	Optional<QueryBuilder> toElasticQuery();

	/**
	 * Defines whether the query returned by {@link #toElasticQuery()} should be negated.
	 *
	 * @return {@code true} if {@code must_not} should be used instead of {@code must}.
	 */
	boolean negateElasticQuery();

	/**
	 * Transforms the time specification to a query that fetches missing dates from the database.
	 *
	 * @param applied
	 *            The dates fetched from the database.
	 * @param step
	 *            The step width.
	 * @return The query.
	 */
	default Optional<QueryBuilder> toPostprocessElasticQuery(List<Date> applied, Duration step) {
		return Optional.empty();
	}

}
