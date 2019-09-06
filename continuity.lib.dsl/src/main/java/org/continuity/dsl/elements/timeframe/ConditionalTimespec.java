package org.continuity.dsl.elements.timeframe;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.dsl.elements.TimeSpecification;
import org.elasticsearch.index.query.QueryBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Applies if one or several conditions based on context variables hold.
 *
 * @author Henning Schulz
 *
 */
public class ConditionalTimespec implements TimeSpecification {

	@JsonValue
	private Map<String, Condition> conditions = new HashMap<>();

	@JsonCreator
	public ConditionalTimespec(Map<String, Condition> conditions) {
		this.conditions = conditions;
	}

	public ConditionalTimespec() {
	}

	public Map<String, Condition> getConditions() {
		return conditions;
	}

	public void setConditions(Map<String, Condition> conditions) {
		this.conditions = conditions;
	}

	@Override
	public boolean appliesToDate(LocalDateTime date) {
		return true;
	}

	@Override
	public boolean appliesToNumerical(String variable, double value) {
		if (conditions == null) {
			return true;
		}

		return conditions.entrySet().stream().map(e -> e.getValue().appliesToNumerical(e.getKey(), variable, value)).reduce(Boolean::logicalAnd).orElse(true);
	}

	@Override
	public boolean appliesToBoolean(Set<String> occurring) {
		if (conditions == null) {
			return true;
		}

		return conditions.entrySet().stream().map(e -> e.getValue().appliesToBoolean(e.getKey(), occurring)).reduce(Boolean::logicalAnd).orElse(true);
	}

	@Override
	public boolean appliesToString(String variable, String value) {
		if (conditions == null) {
			return true;
		}

		return conditions.entrySet().stream().map(e -> e.getValue().appliesToString(e.getKey(), variable, value)).reduce(Boolean::logicalAnd).orElse(true);
	}

	@Override
	public List<Pair<QueryBuilder, Boolean>> toElasticQuery(ZoneId timeZone) {
		if ((conditions == null) || conditions.isEmpty()) {
			return Collections.emptyList();
		}

		return conditions.entrySet().stream().map(e -> Pair.of(e.getValue().toElasticQuery(e.getKey()), !e.getValue().negateQuery())).filter(p -> p.getLeft() != null).collect(Collectors.toList());
	}

}
