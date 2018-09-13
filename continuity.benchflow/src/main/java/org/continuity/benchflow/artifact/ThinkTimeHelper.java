package org.continuity.benchflow.artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.benchflow.dsl.definition.workload.operation.thinktime.ThinkTime;
import scala.Option;

/**
 * Stores a list of think times for each state.
 * 
 * @author Manuel Palenga
 *
 */
public class ThinkTimeHelper {

	private Map<String, List<NormallyDistributedThinkTimeHelper>> mapThinkTimes = new HashMap<String, List<NormallyDistributedThinkTimeHelper>>();
	
	public void initState(String stateId) {
		mapThinkTimes.put(stateId, new ArrayList<NormallyDistributedThinkTimeHelper>());
	}
	
	public void addThinkTimeToState(String stateId, double mean, double deviation) {
		if(!mapThinkTimes.containsKey(stateId)) {
			this.initState(stateId);
		}
		mapThinkTimes.get(stateId).add(new NormallyDistributedThinkTimeHelper(mean, deviation));
	}
	
	public ThinkTime getCalculatedThinkTimeOfState(String stateId) {
		
		if(!mapThinkTimes.containsKey(stateId)) {
			return null;
		}
		
		double sumMean = 0.;
		double sumDeviation = 0.;
		
		int numberOfValues = mapThinkTimes.get(stateId).size();
		
		for (NormallyDistributedThinkTimeHelper thinkTime : mapThinkTimes.get(stateId)) {
			sumMean += thinkTime.getMean();
			sumDeviation += thinkTime.getDeviation();
		}
		
		double resultMean = sumMean / numberOfValues;
		double resultDeviation = sumDeviation / numberOfValues;
		
		return new ThinkTime(resultMean, Option.apply(resultDeviation));
	}
		
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ThinkTimeHelper [Size=" + mapThinkTimes.size() + "]");
		for(String stateId : mapThinkTimes.keySet()) {
			builder.append("\n:> MarkovState [EId=" + stateId + "]");
			for (NormallyDistributedThinkTimeHelper thinkTime : mapThinkTimes.get(stateId)) {
				builder.append(thinkTime.toString());
			}
		}
		return builder.toString();
	}

	private static class NormallyDistributedThinkTimeHelper {
		private double mean;
		private double deviation;
		
		public NormallyDistributedThinkTimeHelper(double mean, double deviation) {
			this.mean = mean;
			this.deviation = deviation;
		}
		
		public double getMean() {
			return mean;
		}
		
		public double getDeviation() {
			return deviation;
		}

		@Override
		public String toString() {
			return "[mean=" + mean + ", deviation=" + deviation + "]";
		}
	}
	
}
