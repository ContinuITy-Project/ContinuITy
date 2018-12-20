package org.continuity.idpa.serialization;

import org.continuity.idpa.annotation.JsonPathExtraction;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.annotation.ValueExtraction;

public class ValueExtractionDeserializer extends UniquePropertyPolymorphicDeserializer<ValueExtraction> {

	private static final long serialVersionUID = 7758011854273993656L;

	public ValueExtractionDeserializer() {
		super(ValueExtraction.class);

		register("pattern", RegExExtraction.class);
		register("json-path", JsonPathExtraction.class);
	}

}
