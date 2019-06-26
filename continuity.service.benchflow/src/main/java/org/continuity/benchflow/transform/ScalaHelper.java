package org.continuity.benchflow.transform;

import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

/**
 * Helps by the transformation of the ContinuITy model into the BenchFlow model.
 * 
 * @author Manuel Palenga
 *
 */
public class ScalaHelper {
	
	/**
	 * Transformed a Java map into a Scala map.
	 * 
	 * @param javaMap 
	 * 					Java map for the transformation.
	 * @return The transformed Scala map.
	 */
	public static <T> Map<String, T> mapAsScalaMap(java.util.Map<String, T> javaMap) {
		if(javaMap == null) {
			return null;
		}
		
		Map<String, T> scalaMap = Map$.MODULE$.empty();	
		for(java.util.Map.Entry<String, T> entry : javaMap.entrySet()) {
			scalaMap = scalaMap.$plus(new Tuple2<String, T>(entry.getKey(), entry.getValue()));
		}
		return scalaMap;
	}
	
	/**
	 * Transformed a Java map into a Option with a Scala map.
	 * 
	 * @param javaMap 
	 * 					Java map for the transformation.
	 * @return The transformed Option with a Scala map.
	 */
	public static <T> Option<Map<String, T>> mapAsOptionScalaMap(java.util.Map<String, T> javaMap) {
		if(javaMap == null) {
			return Option.empty();
		}
		
		Map<String, T> scalaMap = mapAsScalaMap(javaMap);
		if(scalaMap.nonEmpty()) {
			return Option.apply(scalaMap);
		}
		return Option.empty();
	}
}
