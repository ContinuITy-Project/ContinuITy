package open.xtrace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.spec.research.open.xtrace.api.core.SubTrace;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.SubTraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.TraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;

public class OpenXtraceDeSerializationTest {

	@Test
	public void testWriteReadSingle() {
		Trace orig = createTrace(1234, 1000000);

		String json = OPENxtraceUtils.serializeTraceToJsonString(orig);
		Trace parsed = OPENxtraceUtils.deserializeToTrace(json);

		assertThat(parsed).isNotNull();
		assertThat(parsed.getTraceId()).isEqualTo(orig.getTraceId());
		assertThat(parsed.getRoot().getRoot().getTimestamp()).isEqualTo(orig.getRoot().getRoot().getTimestamp());
	}

	@Test
	public void testWriteReadList() {
		List<Trace> traces = Arrays.asList(createTrace(123, 1000000), createTrace(456, 1000000), createTrace(789, 3000000));

		String json = OPENxtraceUtils.serializeTraceListToJsonString(traces);
		List<Trace> parsed = OPENxtraceUtils.deserializeIntoTraceList(json);

		assertThat(parsed).isNotEmpty();
		assertThat(parsed).hasSize(3);
		assertThat(parsed).extracting(Trace::getTraceId).containsExactlyInAnyOrder(123L, 456L, 789L);
		assertThat(parsed).extracting(Trace::getRoot).extracting(SubTrace::getRoot).extracting(Callable::getTimestamp).containsExactlyInAnyOrder(1000000L, 1000000L, 3000000L);
	}

	private Trace createTrace(long id, long timestamp) {
		TraceImpl trace = new TraceImpl(id);
		SubTraceImpl subTrace = new SubTraceImpl();
		subTrace.setIdentifier(id);
		subTrace.setLocation(new LocationImpl());
		HTTPRequestProcessingImpl req = new HTTPRequestProcessingImpl(null, subTrace);
		req.setTimestamp(timestamp);
		subTrace.setRoot(req);
		trace.setRoot(subTrace);

		return trace;
	}

}
