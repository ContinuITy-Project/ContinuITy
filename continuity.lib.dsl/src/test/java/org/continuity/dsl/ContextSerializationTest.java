package org.continuity.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.dsl.adjustment.IntensityIncreasedAdjustment;
import org.continuity.dsl.adjustment.IntensityMultipliedAdjustment;
import org.continuity.dsl.context.Context;
import org.continuity.dsl.context.TimeSpecification;
import org.continuity.dsl.context.WorkloadAdjustment;
import org.continuity.dsl.context.WorkloadInfluence;
import org.continuity.dsl.context.influence.FixedInfluence;
import org.continuity.dsl.context.influence.IncreasedInfluence;
import org.continuity.dsl.context.influence.IsAbsentInfluence;
import org.continuity.dsl.context.influence.MultipliedInfluence;
import org.continuity.dsl.context.influence.OccursInfluence;
import org.continuity.dsl.context.timespec.AbsentSpecification;
import org.continuity.dsl.context.timespec.AfterSpecification;
import org.continuity.dsl.context.timespec.BeforeSpecification;
import org.continuity.dsl.context.timespec.EqualSpecification;
import org.continuity.dsl.context.timespec.LessSpecification;
import org.continuity.dsl.context.timespec.PlusSpecification;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

public class ContextSerializationTest {

	private Context context;

	private ObjectMapper mapper;

	@Before
	public void setup() {
		context = new Context();

		List<TimeSpecification> when = new ArrayList<>();
		EqualSpecification equal = new EqualSpecification();
		equal.setWhat("my_var");
		equal.setTo(new StringOrNumeric(123.4));
		when.add(equal);

		AbsentSpecification absent = new AbsentSpecification();
		absent.setWhat("bool_var");
		when.add(absent);

		AfterSpecification after = new AfterSpecification();
		after.setDate(new Date(0));
		when.add(after);

		LessSpecification less = new LessSpecification();
		less.setWhat("num_var");
		less.setThan(42);
		when.add(less);

		PlusSpecification plus = new PlusSpecification();
		plus.setDuration(Duration.ofHours(1));
		when.add(plus);

		context.setWhen(when);

		Map<String, List<WorkloadInfluence>> influences = new HashMap<>();
		FixedInfluence fixed = new FixedInfluence();
		fixed.setValue(new StringOrNumeric("foo"));
		influences.put("other_var", Collections.singletonList(fixed));

		OccursInfluence occurs = new OccursInfluence();
		IsAbsentInfluence isAbsent = new IsAbsentInfluence();
		BeforeSpecification absentBefore = new BeforeSpecification();
		absentBefore.setDate(new Date(1000000));
		isAbsent.setWhen(Collections.singletonList(absentBefore));
		influences.put("bool_var", Arrays.asList(occurs, isAbsent));

		MultipliedInfluence multiplied = new MultipliedInfluence();
		multiplied.setWith(1.3);
		IncreasedInfluence increased = new IncreasedInfluence();
		increased.setBy(5);
		influences.put("num_var", Arrays.asList(multiplied, increased));

		context.setInfluencing(influences);

		List<WorkloadAdjustment> adjustments = new ArrayList<>();
		IntensityMultipliedAdjustment intMultiplied = new IntensityMultipliedAdjustment();
		intMultiplied.setWith(1.5);
		adjustments.add(intMultiplied);

		IntensityIncreasedAdjustment intIncreased = new IntensityIncreasedAdjustment();
		intIncreased.setBy(200);
		intIncreased.setGroup(2);
		adjustments.add(intIncreased);

		context.setAdjusted(adjustments);

		mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
	}

	@Test
	public void testWriteRead() throws IOException {
		String yaml = mapper.writeValueAsString(context);

		System.out.println(yaml);

		Context parsed = mapper.readValue(yaml, Context.class);

		assertThat(parsed.getWhen()).extracting(TimeSpecification::getClass).extracting(Class::toString)
				.containsExactlyElementsOf(context.getWhen().stream().map(TimeSpecification::getClass).map(Class::toString).collect(Collectors.toList()));

		assertThat(parsed.getInfluencing().keySet()).containsExactlyElementsOf(context.getInfluencing().keySet());
		assertThat(parsed.getInfluencing().values()).flatExtracting(t -> t).extracting(WorkloadInfluence::getClass).extracting(Class::toString)
				.containsExactlyElementsOf(context.getInfluencing().values().stream().flatMap(List::stream).map(WorkloadInfluence::getClass).map(Class::toString).collect(Collectors.toList()));

		assertThat(parsed.getAdjusted()).extracting(WorkloadAdjustment::getClass).extracting(Class::toString)
				.containsExactlyElementsOf(context.getAdjusted().stream().map(WorkloadAdjustment::getClass).map(Class::toString).collect(Collectors.toList()));

		assertThat(mapper.writeValueAsString(parsed)).isEqualTo(yaml);

	}

}
