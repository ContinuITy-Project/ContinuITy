package org.continuity.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.continuity.dsl.elements.ContextSpecification;
import org.continuity.dsl.elements.TimeSpecification;
import org.continuity.dsl.elements.TypedProperties;
import org.continuity.dsl.elements.timeframe.Condition;
import org.continuity.dsl.elements.timeframe.ConditionalTimespec;
import org.continuity.dsl.elements.timeframe.ExtendingTimespec;
import org.continuity.dsl.elements.timeframe.Timerange;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ContextSerializationTest {

	private WorkloadDescription description;

	private ObjectMapper yamlMapper;

	private ObjectMapper jsonMapper;

	@Before
	public void setup() {
		description = new WorkloadDescription();

		List<TimeSpecification> timeframe = new ArrayList<>();

		ConditionalTimespec conditional = new ConditionalTimespec();
		conditional.getConditions().put("my_var", new Condition().setIs(new ContextValue(123.4)));
		conditional.getConditions().put("bool_var", new Condition().setIs(new ContextValue(false)));
		timeframe.add(conditional);

		Timerange timerange = new Timerange().setFrom(LocalDateTime.of(2019, 9, 1, 0, 0));
		timeframe.add(timerange);

		ConditionalTimespec conditional2 = new ConditionalTimespec();
		conditional2.getConditions().put("num_var", new Condition().setLess(42D));
		timeframe.add(conditional2);

		ExtendingTimespec extending = new ExtendingTimespec().setEnd(Duration.ofHours(1));
		timeframe.add(extending);

		description.setTimeframe(timeframe);

		Map<String, List<ContextSpecification>> context = new HashMap<>();

		context.put("other_var", Collections.singletonList(new ContextSpecification().setIs(new ContextValue("foo"))));

		ContextSpecification occurs = new ContextSpecification().setIs(new ContextValue(true));
		ContextSpecification isAbsent = new ContextSpecification().setIs(new ContextValue(false)).setDuring(Collections.singletonList(new Timerange().setTo(LocalDateTime.of(2019, 9, 3, 0, 0))));
		context.put("bool_var", Arrays.asList(occurs, isAbsent));

		context.put("num_var", Collections.singletonList(new ContextSpecification().setMultiplied(1.3).setAdded(5D)));

		description.setContext(context);

		description.setAggregation(new TypedProperties().setType("percentile").setProperties(Collections.singletonMap("p", 95)));

		TypedProperties usersMultiplied = new TypedProperties().setType("users-multiplied");
		TypedProperties usersAdded = new TypedProperties().setType("users-added").setProperties(Collections.singletonMap("group", "2"));
		description.setAdjustments(Arrays.asList(usersMultiplied, usersAdded));

		yamlMapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID)).registerModule(new Jdk8Module()).registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		jsonMapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Test
	public void testWriteReadYaml() throws IOException {
		testWriteRead(yamlMapper);
	}

	@Test
	public void testWriteReadJson() throws IOException {
		testWriteRead(jsonMapper);
	}

	public void testWriteRead(ObjectMapper mapper) throws IOException {
		String serialized = mapper.writeValueAsString(description);

		System.out.println(serialized);

		WorkloadDescription parsed = mapper.readValue(serialized, WorkloadDescription.class);

		assertThat(parsed.getTimeframe()).extracting(TimeSpecification::getClass).extracting(Class::toString)
				.containsExactlyElementsOf(description.getTimeframe().stream().map(TimeSpecification::getClass).map(Class::toString).collect(Collectors.toList()));

		assertThat(parsed.getContext().keySet()).containsExactlyElementsOf(description.getContext().keySet());
		assertThat(parsed.getContext().values()).flatExtracting(t -> t).extracting(ContextSpecification::getClass).extracting(Class::toString)
				.containsExactlyElementsOf(description.getContext().values().stream().flatMap(List::stream).map(ContextSpecification::getClass).map(Class::toString).collect(Collectors.toList()));

		assertThat(parsed.getAggregation().getType()).isEqualTo(description.getAggregation().getType());
		assertThat(parsed.getAggregation().getProperties()).isEqualTo(description.getAggregation().getProperties());

		assertThat(parsed.getAdjustments()).extracting(TypedProperties::getType).isEqualTo(description.getAdjustments().stream().map(TypedProperties::getType).collect(Collectors.toList()));
		assertThat(parsed.getAdjustments()).extracting(TypedProperties::getProperties).filteredOn(Objects::nonNull).flatExtracting(Map::keySet).containsExactlyElementsOf(
				description.getAdjustments().stream().map(TypedProperties::getProperties).filter(Objects::nonNull).map(Map::keySet).flatMap(Set::stream).collect(Collectors.toList()));

		assertThat(mapper.writeValueAsString(parsed)).isEqualTo(serialized);

	}

}
