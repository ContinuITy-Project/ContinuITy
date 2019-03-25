package org.continuity.idpa.serialization;

/**
 *
 * Adjusts a yaml String for proper deserialization with Jackson. To be called before putting the
 * yaml String into the deserializer.
 *
 * @author Henning Schulz
 *
 */
@FunctionalInterface
public interface PreDeserializationSanitizer {

	String sanitize(String yaml);

}
