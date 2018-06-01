package org.continuity.api.entities.artifact;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.ListedHashTree;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @author Henning Schulz
 *
 */
public class JMeterTestPlanSerializer extends StdSerializer<ListedHashTree> {

	/**
	 *
	 */
	private static final long serialVersionUID = 4111003643037164308L;

	/**
	 * Default constructor.
	 */
	public JMeterTestPlanSerializer() {
		this(null);
	}

	/**
	 * @param t
	 */
	protected JMeterTestPlanSerializer(Class<ListedHashTree> t) {
		super(t);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(ListedHashTree value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(toTestPlanJxm(value));
	}

	private String toTestPlanJxm(ListedHashTree testPlan) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			SaveService.saveTree(testPlan, out);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			return new String(out.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

}
