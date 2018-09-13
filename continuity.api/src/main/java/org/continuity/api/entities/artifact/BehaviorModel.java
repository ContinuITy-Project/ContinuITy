package org.continuity.api.entities.artifact;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author Manuel Palenga
 *
 */
public class BehaviorModel {

	@JsonInclude(Include.NON_NULL)
	private List<Behavior> behaviors;
	
	public void setBehaviors(List<Behavior> behaviors) {
		this.behaviors = behaviors;
	}
	
	public List<Behavior> getBehaviors() {
		return behaviors;
	}
	
	public static class Behavior {
		
		@JsonInclude(Include.NON_NULL)
		private String name;
		
		@JsonInclude(Include.NON_NULL)
		private String initialState;
		
		@JsonInclude(Include.NON_NULL)
		private Double probability;
		
		@JsonProperty(value = "markov-states")
		@JsonInclude(Include.NON_NULL)
		private List<MarkovState> markovStates;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getInitialState() {
			return initialState;
		}
		
		public void setInitialState(String initialState) {
			this.initialState = initialState;
		}
		
		public Double getProbability() {
			return probability;
		}
		
		public void setProbability(Double probability) {
			this.probability = probability;
		}
		
		public List<MarkovState> getMarkovStates() {
			return markovStates;
		}
		
		public void setMarkovStates(List<MarkovState> markovStates) {
			this.markovStates = markovStates;
		}
	}
	
	public static class MarkovState {
		
		@JsonProperty(required = true)
		@JsonInclude(Include.NON_NULL)
		private String id;
		
		@JsonInclude(Include.NON_NULL)
		private List<Transition> transitions;
		
		public String getId() {
			return id;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		
		public List<Transition> getTransitions() {
			return transitions;
		}
		
		public void setTransitions(List<Transition> transitions) {
			this.transitions = transitions;
		}
		
	}
	
	public static class Transition {
		
		@JsonProperty(required = true)
		@JsonInclude(Include.NON_NULL)
		private String targetState;
		
		@JsonInclude(Include.NON_NULL)
		private Double probability;
		
		@JsonProperty(value = "think-time-mean")
		@JsonInclude(Include.NON_NULL)
		private Double mean;
		
		@JsonProperty(value = "think-time-deviation")
		@JsonInclude(Include.NON_NULL)
		private Double deviation;

		public String getTargetState() {
			return targetState;
		}

		public void setTargetState(String targetState) {
			this.targetState = targetState;
		}

		public Double getProbability() {
			return probability;
		}

		public void setProbability(Double probability) {
			this.probability = probability;
		}

		public Double getMean() {
			return mean;
		}

		public void setMean(Double mean) {
			this.mean = mean;
		}

		public Double getDeviation() {
			return deviation;
		}

		public void setDeviation(Double deviation) {
			this.deviation = deviation;
		}
	}
	
}
