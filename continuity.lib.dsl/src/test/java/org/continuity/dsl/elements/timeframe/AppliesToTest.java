package org.continuity.dsl.elements.timeframe;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.dsl.ContextValue;
import org.continuity.dsl.timeseries.ContextRecord;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.dsl.timeseries.NumericVariable;
import org.continuity.dsl.timeseries.StringVariable;
import org.junit.Before;
import org.junit.Test;

public class AppliesToTest {

	private static final long TIMESTAMP = 1234;

	private static final ZoneId TIMEZONE = ZoneId.systemDefault();

	private IntensityRecord empty;

	private IntensityRecord withString;

	private IntensityRecord withBoolean;

	private IntensityRecord withPositive;

	private IntensityRecord withZero;

	private IntensityRecord withAll;

	@Before
	public void setup() {
		empty = newRecord();

		withString = newRecord();
		withString(withString, new StringVariable("s1", "foo", withString.getContext()), new StringVariable("s2", "bar", withString.getContext()));

		withBoolean = newRecord();
		withBoolean(withBoolean, "bool");

		withPositive = newRecord();
		withNumeric(withPositive, new NumericVariable("num", 5, withPositive.getContext()));

		withZero = newRecord();
		withNumeric(withZero, new NumericVariable("num", 0, withZero.getContext()));

		withAll = newRecord();
		withString(withAll, new StringVariable("s1", "foo", withAll.getContext()), new StringVariable("s2", "bar", withAll.getContext()));
		withBoolean(withAll, "bool");
		withNumeric(withAll, new NumericVariable("num", 5, withAll.getContext()), new NumericVariable("num2", 0, withAll.getContext()));
	}

	private IntensityRecord newRecord() {
		IntensityRecord record = new IntensityRecord(TIMESTAMP);
		ContextRecord context = new ContextRecord();
		record.setContext(context);
		return record;
	}

	private void withString(IntensityRecord record, StringVariable... vars) {
		record.getContext().setString(Arrays.stream(vars).collect(Collectors.toMap(StringVariable::getName, StringVariable::getValue)));
	}

	private void withNumeric(IntensityRecord record, NumericVariable... vars) {
		record.getContext().setNumeric(Arrays.stream(vars).collect(Collectors.toMap(NumericVariable::getName, NumericVariable::getValue)));
	}

	private void withBoolean(IntensityRecord record, String... vars) {
		record.getContext().setBoolean(Arrays.stream(vars).collect(Collectors.toSet()));
	}

	@Test
	public void testIsString() {
		assertThat(conditional().variable("s1").is(new ContextValue("foo")).build().appliesTo(empty, TIMEZONE)).isFalse();

		assertThat(conditional().variable("s1").is(new ContextValue("foo")).build().appliesTo(withString, TIMEZONE)).isTrue();
		assertThat(conditional().variable("s1").is(new ContextValue("foo")).variable("s2").is(new ContextValue("bar")).build().appliesTo(withString, TIMEZONE)).isTrue();
		assertThat(conditional().variable("s1").is(new ContextValue("bar")).build().appliesTo(withString, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").is(new ContextValue("foo")).build().appliesTo(withString, TIMEZONE)).isFalse();

		assertThat(conditional().variable("s1").is(new ContextValue("foo")).build().appliesTo(withBoolean, TIMEZONE)).isFalse();

		assertThat(conditional().variable("s1").is(new ContextValue("foo")).build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("s1").is(new ContextValue("bar")).build().appliesTo(withAll, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").is(new ContextValue("foo")).build().appliesTo(withAll, TIMEZONE)).isFalse();
	}

	@Test
	public void testIsNumeric() {
		assertThat(conditional().variable("num").is(new ContextValue(5)).build().appliesTo(empty, TIMEZONE)).isFalse();

		assertThat(conditional().variable("num").is(new ContextValue(5)).build().appliesTo(withPositive, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").is(new ContextValue(3)).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").is(new ContextValue(5)).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").is(new ContextValue(0)).build().appliesTo(withPositive, TIMEZONE)).isTrue();

		assertThat(conditional().variable("undefined").is(new ContextValue(5)).build().appliesTo(withBoolean, TIMEZONE)).isFalse();

		assertThat(conditional().variable("num").is(new ContextValue(5)).build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").is(new ContextValue(3)).build().appliesTo(withAll, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").is(new ContextValue(5)).variable("num2").is(new ContextValue(0)).build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("undefined").is(new ContextValue(5)).build().appliesTo(withAll, TIMEZONE)).isFalse();
	}

	@Test
	public void testIsBoolean() {
		assertThat(conditional().variable("bool").is(new ContextValue(true)).build().appliesTo(empty, TIMEZONE)).isFalse();
		assertThat(conditional().variable("bool").is(new ContextValue(false)).build().appliesTo(empty, TIMEZONE)).isTrue();

		assertThat(conditional().variable("bool").is(new ContextValue(true)).build().appliesTo(withBoolean, TIMEZONE)).isTrue();
		assertThat(conditional().variable("bool").is(new ContextValue(false)).build().appliesTo(withBoolean, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").is(new ContextValue(true)).build().appliesTo(withBoolean, TIMEZONE)).isFalse();

		assertThat(conditional().variable("bool").is(new ContextValue(true)).build().appliesTo(withString, TIMEZONE)).isFalse();
		assertThat(conditional().variable("bool").is(new ContextValue(false)).build().appliesTo(withString, TIMEZONE)).isTrue();

		assertThat(conditional().variable("bool").is(new ContextValue(true)).build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("bool").is(new ContextValue(false)).build().appliesTo(withAll, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").is(new ContextValue(true)).build().appliesTo(withAll, TIMEZONE)).isFalse();
	}

	@Test
	public void testComparison() {
		assertThat(conditional().variable("num").comparing(1.0, 5.0).build().appliesTo(empty, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(0.0, 5.0).build().appliesTo(empty, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(-5.0, 0.0).build().appliesTo(empty, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(-5.0, -1.0).build().appliesTo(empty, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(null, 0.0).build().appliesTo(empty, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(0.0, null).build().appliesTo(empty, TIMEZONE)).isTrue();

		assertThat(conditional().variable("num").comparing(1.0, 5.0).build().appliesTo(withZero, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(0.0, 5.0).build().appliesTo(withZero, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(-5.0, 0.0).build().appliesTo(withZero, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(-5.0, -1.0).build().appliesTo(withZero, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(null, 0.0).build().appliesTo(withZero, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(0.0, null).build().appliesTo(withZero, TIMEZONE)).isTrue();

		assertThat(conditional().variable("undefined").comparing(1.0, 5.0).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").comparing(0.0, 5.0).build().appliesTo(withPositive, TIMEZONE)).isTrue();
		assertThat(conditional().variable("undefined").comparing(-5.0, 0.0).build().appliesTo(withPositive, TIMEZONE)).isTrue();
		assertThat(conditional().variable("undefined").comparing(-5.0, -1.0).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").comparing(null, 0.0).build().appliesTo(withPositive, TIMEZONE)).isTrue();
		assertThat(conditional().variable("undefined").comparing(0.0, null).build().appliesTo(withPositive, TIMEZONE)).isTrue();

		assertThat(conditional().variable("num").comparing(1.0, 5.0).build().appliesTo(withPositive, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(0.0, 5.0).build().appliesTo(withPositive, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(-5.0, 0.0).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(-5.0, -1.0).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(null, 0.0).build().appliesTo(withPositive, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(0.0, null).build().appliesTo(withPositive, TIMEZONE)).isTrue();

		assertThat(conditional().variable("num").comparing(1.0, 5.0).build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(0.0, 5.0).build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("num").comparing(-5.0, 0.0).build().appliesTo(withAll, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(-5.0, -1.0).build().appliesTo(withAll, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(null, 0.0).build().appliesTo(withAll, TIMEZONE)).isFalse();
		assertThat(conditional().variable("num").comparing(0.0, null).build().appliesTo(withAll, TIMEZONE)).isTrue();
	}

	@Test
	public void testExists() {
		assertThat(conditional().variable("s1").exists().build().appliesTo(withString, TIMEZONE)).isTrue();
		assertThat(conditional().variable("s2").exists().build().appliesTo(withString, TIMEZONE)).isTrue();
		assertThat(conditional().variable("undefined").exists().build().appliesTo(withString, TIMEZONE)).isFalse();

		assertThat(conditional().variable("s1").notExists().build().appliesTo(withString, TIMEZONE)).isFalse();
		assertThat(conditional().variable("s2").notExists().build().appliesTo(withString, TIMEZONE)).isFalse();
		assertThat(conditional().variable("undefined").notExists().build().appliesTo(withString, TIMEZONE)).isTrue();

		assertThat(conditional().variable("s1").exists().build().appliesTo(withBoolean, TIMEZONE)).isFalse();
		assertThat(conditional().variable("s1").notExists().build().appliesTo(withBoolean, TIMEZONE)).isTrue();

		assertThat(conditional().variable("s1").exists().build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("s2").exists().build().appliesTo(withAll, TIMEZONE)).isTrue();
		assertThat(conditional().variable("undefined").exists().build().appliesTo(withAll, TIMEZONE)).isFalse();
	}

	private Builder conditional() {
		return new Builder();
	}

	private class Builder {

		private final Map<String, Condition> conditions = new HashMap<>();

		private String currVar;

		private Builder variable(String var) {
			currVar = var;
			return this;
		}

		private Builder is(ContextValue value) {
			Condition cond = new Condition();
			cond.setIs(value);
			conditions.put(currVar, cond);
			return this;
		}

		private Builder comparing(Double greater, Double less) {
			Condition cond = new Condition();
			cond.setGreater(greater);
			cond.setLess(less);
			conditions.put(currVar, cond);
			return this;
		}

		private Builder exists() {
			Condition cond = new Condition();
			cond.setExists(true);
			conditions.put(currVar, cond);
			return this;
		}

		private Builder notExists() {
			Condition cond = new Condition();
			cond.setExists(false);
			conditions.put(currVar, cond);
			return this;
		}

		private ConditionalTimespec build() {
			return new ConditionalTimespec(conditions);
		}

	}

}
