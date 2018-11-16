package org.continuity.dsl.main;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.continuity.dsl.description.ForecastInput;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Testing purposes.
 * 
 * @author Alper Hidiroglu
 *
 */
public class Main {

	/**
	 * Test of YAML to object mapping.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		try {

			ForecastInput descr = mapper.readValue(new File("C:/Users/ahi/Desktop/ContextDescriptions/context.yaml"),
					ForecastInput.class);

			System.out.println(ReflectionToStringBuilder.toString(descr,
			 ToStringStyle.MULTI_LINE_STYLE));
			
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			String date = Instant.ofEpochMilli(1335391200000L).atOffset(ZoneOffset.UTC).format(dtf).toString();
		    System.out.println(date.toString());

			// StringCovariate covar = (StringCovariate) descr.getCovariates().get(0);

		} catch (Exception e) {

			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}
}
