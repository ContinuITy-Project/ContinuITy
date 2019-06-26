package org.continuity.benchflow.artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.benchflow.dsl.BenchFlowTestAPI;
import cloud.benchflow.dsl.definition.BenchFlowTest;
import cloud.benchflow.dsl.definition.errorhandling.BenchFlowDeserializationException;
import cloud.benchflow.dsl.definition.sut.sutversion.Version;
import cloud.benchflow.dsl.definition.workload.HttpWorkload;
import cloud.benchflow.dsl.definition.workload.Workload;
import cloud.benchflow.dsl.definition.workload.WorkloadYamlProtocol;
import net.jcazevedo.moultingyaml.YamlValue;
import scala.collection.Iterator;

/**
 * 
 * Helper class for using BenchFlow.
 * 
 * @author Manuel Palenga
 *
 */
public class BenchFlowUtility {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchFlowUtility.class);
	
	/**
	 * Returns a valid version from the provided version if available, otherwise the result is a {@link IllegalArgumentException}.
	 * 
	 * @param version
	 * 				Contains version information.
	 * @return The extracted version.
	 */
	public static String extractVersion(String version) {
		// Remove all spaces
		String tempVersion = version.replace(" ", "");
		
		// Pattern matching
		Pattern versionPattern = Pattern.compile("[0-9]+(\\.[0-9]+)*");
		Matcher versionMatcher = versionPattern.matcher(tempVersion);
		
		if (!versionMatcher.find()){
			// No valid version found
			throw new IllegalArgumentException(version + " does not contain a valid version. Example of a correct form: '1.23.456'");
		}
		return versionMatcher.group();
	}
	
	/**
	 * Transforms the provided workload string into a {@link HttpWorkload} from BenchFlow.
	 * 
	 * @param workload
	 * 			The workload which should be transformed.
	 * @return The transformed HTTP workload.
	 */
	public static HttpWorkload loadHttpWorkloadFromString(String workload) {
		WorkloadYamlProtocol.WorkloadReadFormat$ reader = new WorkloadYamlProtocol.WorkloadReadFormat$();
		Workload benchFlowWorkload;
		try {
			benchFlowWorkload = reader.workloadFromYaml(workload);
		} catch (BenchFlowDeserializationException e) {
			LOGGER.error("Exception during parsing BenchFlow workload DSL!", e);
			throw new IllegalArgumentException("Exception during parsing BenchFlow DSL!", e);
		}
		if(!(benchFlowWorkload instanceof HttpWorkload)) {
			LOGGER.error("Workload is not an HTTP workload!");
			throw new IllegalArgumentException("Workload is not an HTTP workload!");
		}
		return (HttpWorkload) benchFlowWorkload;
	}
	
	/**
	 * Transforms the provided BenchFlow DSL string into a {@link BenchFlowTest}.
	 * 
	 * @param benchFlowDSL
	 * 			The BenchFlow DSL which should be transformed.
	 * @return The transformed {@link BenchFlowTest}.
	 */
	public static BenchFlowTest loadBenchFlowTestFromString(String benchFlowDSL) {
		try {
			return BenchFlowTestAPI.testFromYaml(benchFlowDSL);
		} catch (BenchFlowDeserializationException e) {
			LOGGER.error("Exception during parsing BenchFlow DSL!", e);
			throw new IllegalArgumentException("Exception during parsing BenchFlow DSL!", e);
		}
	}
	
	/**
	 * Transforms the provided {@link HttpWorkload} into a YAML string.
	 * 
	 * @param httpWorkload
	 * 			The HTTP workload which should be transformed.
	 * @return The transformed YAML string.
	 */
	public static String getStringFromHttpWorkload(HttpWorkload httpWorkload) {
		WorkloadYamlProtocol.WorkloadWriteFormat$ writer = new WorkloadYamlProtocol.WorkloadWriteFormat$();
		YamlValue resultConfiguration = writer.write(httpWorkload);
		return resultConfiguration.prettyPrint();
	}
	
	/**
	 * Returns a list of specified versions in the provided {@link BenchFlowTest}.
	 * @param benchFlowTest
	 * 				BenchFlow test with SUT versions.
	 * @return The specified versions.
	 */
	public static List<String> getVersions(BenchFlowTest benchFlowTest){
		List<String> listVersions = new ArrayList<String>();
		Iterator<Version> iterator = benchFlowTest.sut().version().iterator();
		while(iterator.hasNext()) {
			String versionName = iterator.next().toString();
			listVersions.add(versionName);
		}
		return listVersions;
	}
}
