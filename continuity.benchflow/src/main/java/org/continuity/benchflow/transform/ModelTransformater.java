package org.continuity.benchflow.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;
import org.continuity.benchflow.artifact.ContinuITyModel;
import org.continuity.benchflow.artifact.HttpParameterBundle;
import org.continuity.benchflow.artifact.ThinkTimeHelper;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.JsonPathExtraction;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.annotation.ValueExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.benchflow.dsl.definition.types.percent.Percent;
import cloud.benchflow.dsl.definition.workload.HttpWorkload;
import cloud.benchflow.dsl.definition.workload.drivertype.DriverType;
import cloud.benchflow.dsl.definition.workload.interoperationtimingstype.InterOperationsTimingType;
import cloud.benchflow.dsl.definition.workload.mix.MatrixMix;
import cloud.benchflow.dsl.definition.workload.mix.Mix;
import cloud.benchflow.dsl.definition.workload.operation.Operation;
import cloud.benchflow.dsl.definition.workload.operation.body.Body;
import cloud.benchflow.dsl.definition.workload.operation.body.BodyForm;
import cloud.benchflow.dsl.definition.workload.operation.extraction.Extraction;
import cloud.benchflow.dsl.definition.workload.operation.method.Method;
import cloud.benchflow.dsl.definition.workload.operation.parameter.Parameter;
import cloud.benchflow.dsl.definition.workload.operation.protocol.Protocol;
import cloud.benchflow.dsl.definition.workload.operation.thinktime.ThinkTime;
import cloud.benchflow.dsl.definition.workload.workloaditem.HttpWorkloadItem;
import scala.Option;
import scala.collection.JavaConverters;
import scala.collection.Seq;


/**
 * Transformation of the ContinuITy model into the BenchFlow model.
 * 
 * @author Manuel Palenga
 *
 */
public class ModelTransformater {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelTransformater.class);
	
	private static final String START_STATE_BEHAVIOR_MODEL = "INITIAL";
	private static final String START_STATE_DSL = "INITIAL_STATE";
	
	/**
	 * Transformation of the provided ContinuITy model into the BenchFlow model.
	 * 
	 * @param continuITyModel
	 * 							Model for the transformation.
	 * @return The transformed model.
	 */
	public HttpWorkload transformToBenchFlow(ContinuITyModel continuITyModel) {

		if(continuITyModel == null) {
			throw new IllegalArgumentException("A ContinuITy model is requried for the transformation!");
		}
		if(continuITyModel.getBehaviorModel() == null || continuITyModel.getAnnotation() == null || continuITyModel.getApplication() == null) {
			throw new IllegalArgumentException("Some required models from the ContinuITy model are not set!");
		}
		if(continuITyModel.getBehaviorModel().getBehaviors() == null) {
			throw new IllegalArgumentException("No behaviors found!");
		}
		
		Map<String, HttpWorkloadItem> workloadItems = new HashMap<String, HttpWorkloadItem>();
		for(Behavior currentBehavior : continuITyModel.getBehaviorModel().getBehaviors()) {
			
			LOGGER.debug("Parse behavior '{}'", currentBehavior.getName());
			
			ThinkTimeHelper thinkTimeHelper = new ThinkTimeHelper();		
			Mix mix = getBehavior(currentBehavior, thinkTimeHelper);
			
			LOGGER.debug("Transformation of operations from behavior '{}'", currentBehavior.getName());
			List<Operation> operations = getOperations(continuITyModel, currentBehavior, thinkTimeHelper);
			
			Option<Percent> probability = Option.empty();
			if(currentBehavior.getProbability() != null && continuITyModel.getBehaviorModel().getBehaviors().size() != 1) {
				probability = Option.apply(new Percent(currentBehavior.getProbability()));
			}
			
			HttpWorkloadItem workloadItem = new HttpWorkloadItem(
					DriverType.HTTP, 
					probability, 
					Option.apply(InterOperationsTimingType.FIXED_TIME), 
					Option.apply(mix), 
					Option.apply(JavaConverters.asScalaIteratorConverter(operations.iterator()).asScala().toList()),
					Option.empty());			

			workloadItems.put(currentBehavior.getName(), workloadItem);
		}
				
		return new HttpWorkload(ScalaHelper.mapAsScalaMap(workloadItems), Option.empty(), Option.empty(), Option.empty());
	}
	
	/**
	 * Orders the Markov states from the provided behavior so that the initial state is on the first position.
	 * 
	 * @param behavior
	 * 			The behavior for the sorting.
	 */
	private void orderStatesInTheBehavior(Behavior behavior) {
		
		String initialState = behavior.getInitialState();
		
		if(initialState == null) {
			String exceptionMessage = String.format("Initial state is missing in behavior '%s'!", behavior.getName());
			LOGGER.error(exceptionMessage);
			throw new IllegalArgumentException(exceptionMessage);
		}
		
		for(MarkovState markovState : behavior.getMarkovStates()) {				
			if(markovState.getId().equals(initialState)) {
				behavior.getMarkovStates().remove(markovState);
				behavior.getMarkovStates().add(0, markovState);
				break;
			}
		}
	}
	
	private Mix getBehavior(Behavior behavior, ThinkTimeHelper thinkTimeHelper) {
			
		if(behavior.getMarkovStates() == null) {
			throw new IllegalArgumentException("Behavior without markov states found!");
		}

		this.orderStatesInTheBehavior(behavior);
		
		List<Seq<Percent>> matrixMix = new ArrayList<Seq<Percent>>();
		
		for(MarkovState markovState : behavior.getMarkovStates()) {		

			List<Percent> probability = new ArrayList<Percent>();
			
			List<Transition> transitions = markovState.getTransitions();
			if(transitions == null || transitions.isEmpty()) {
				for(int i = 0; i < behavior.getMarkovStates().size(); i++) {
					probability.add(new Percent(0));
				}
				
				Seq<Percent> stateTransitionMix = JavaConverters.asScalaIteratorConverter(probability.iterator()).asScala().toSeq();
				matrixMix.add(stateTransitionMix);			
				continue;
			}

			/*
			 * Add for each markov state probabilities for the transition to another markov state. 
			 */
			for(MarkovState passMarkovState : behavior.getMarkovStates()) {

				Transition connectedTransition = transitions.stream().filter(t -> t.getTargetState().equals(passMarkovState.getId())).findAny().orElse(null);
				if(connectedTransition == null) {
					/*
					 *  If no transition was found for a transition of a pair of markov states then there is no connection between both. 
					 */
					probability.add(new Percent(0));	
				} else {
					
					probability.add(new Percent(connectedTransition.getProbability()));	
					if(connectedTransition.getMean() != null) {
						double thinkTimeMean = connectedTransition.getMean();
						Double deviation = connectedTransition.getDeviation();
						double thinkTimeDeviation = (deviation == null) ? 0 : deviation;
						
						thinkTimeHelper.addThinkTimeToState(passMarkovState.getId(), thinkTimeMean, thinkTimeDeviation);		
					}						
				}
			}				
	
			Seq<Percent> stateTransitionMix = JavaConverters.asScalaIteratorConverter(probability.iterator()).asScala().toSeq();
			matrixMix.add(stateTransitionMix);		
		}
		
		Seq<Seq<Percent>> mixProbabilities = JavaConverters.asScalaIteratorConverter(matrixMix.iterator()).asScala().toSeq();
		return new Mix(Option.empty(), new MatrixMix(mixProbabilities));
	}
	
	/**
	 * Returns a list of all available regex extractions in the provided annotation model.
	 * 
	 * @param annotation
	 * 					Annotation model with possible regex extractions.
	 * @return List of all regex extractions from the annotation model.
	 */
	private List<ExtractedInput> getExtractedInputs(ApplicationAnnotation annotation){
		
		List<ExtractedInput> listExtractedInputs = new ArrayList<ExtractedInput>();
		
		for(Input input : annotation.getInputs()) {
			if(input instanceof ExtractedInput) {
				ExtractedInput extracted = (ExtractedInput) input;
				listExtractedInputs.add(extracted);
			}
		}
		
		return listExtractedInputs;
	}
	
	/**
	 * Returns a list of all state ids in a behavior.
	 * 
	 * @param behavior
 * 						Behavior for search.
	 * @return A list of state ids.
	 */
	private List<String> getAllStates(Behavior behavior){
		List<String> states = new ArrayList<String>();
		for(MarkovState state : behavior.getMarkovStates()) {
			states.add(state.getId());
		}
		
		return states;
	}
	
	private HttpEndpoint getHttpEndpoint(Application application, String id) {
		for(Endpoint<?> endpoint : application.getEndpoints()) {
			if(endpoint instanceof HttpEndpoint) {
				HttpEndpoint httpEndpoint = (HttpEndpoint) endpoint;
				if(httpEndpoint.getId().equals(id)) {
					return httpEndpoint;
				}
			}
		}
		throw new IllegalArgumentException(String.format("HttpEndpoint with id {} not found!", id)) ;
	}
	
	private List<Operation> getOperations(ContinuITyModel continuITyModel, Behavior currentBehavior, ThinkTimeHelper thinkTimeHelper) {
		
		List<Operation> operations = new ArrayList<Operation>();

		List<String> states = this.getAllStates(currentBehavior);
		List<ExtractedInput> allExtractedInputs = this.getExtractedInputs(continuITyModel.getAnnotation());

		for(String serviceName : states) {		
			
			LOGGER.debug("Transform state with service name '{}'", serviceName);
			
			// Map WESSBAS model to annotation model and find related endpoint annotation
			Optional<EndpointAnnotation> optEndpointAnnotation = continuITyModel.getAnnotation().getEndpointAnnotations().stream().filter(
					endpointAnnotation -> endpointAnnotation.getAnnotatedEndpoint().getId().equals(serviceName)).findAny();
			
			HttpEndpoint endpoint = null;
			HttpParameterBundle parameterBundle = null;
			String endpointUrl = null;
			
			if(!optEndpointAnnotation.isPresent()) {

				// WESSBAS contains a start state which is not a request, it contains a list of start requests.
				if(serviceName.equals(START_STATE_BEHAVIOR_MODEL)) {
					Operation initialOperation = new Operation(START_STATE_DSL, null, null, null, 
							Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty());
					
					operations.add(0, initialOperation);
					
					continue;
				}
				
				LOGGER.debug("Endpoint '{}' not found as IDPA application model!", serviceName);
				
				// If an endpoint is not in the annotation model but only in the application model
				try {
					endpoint = getHttpEndpoint(continuITyModel.getApplication(), serviceName);
				} catch(IllegalArgumentException e) {
					throw new IllegalArgumentException("Endpoint '" + serviceName + "' from the WESSBAS model not found in the IDPA model!", e); 
				}
				parameterBundle = new HttpParameterBundle();
				
			} else {
				endpoint = getHttpEndpoint(continuITyModel.getApplication(), serviceName);
				LOGGER.debug("Transform parameters of state with service name '{}'.", serviceName);		
				parameterBundle = this.extractParameters(continuITyModel.getApplication(), optEndpointAnnotation.get());
				
				if(!parameterBundle.getUrlParameter().isEmpty()) {
					endpointUrl = endpoint.getPath();
					for(String parameterKeys : parameterBundle.getUrlParameter().keySet()) {
						endpointUrl = endpointUrl.replaceAll("\\{\\s*" + parameterKeys + "\\s*\\}", "\\${" + parameterKeys + "}");
					}	
				}
			}
								
			Map<String, Extraction> jsonExtraction = this.getExtractions(continuITyModel.getApplication(), allExtractedInputs, endpoint.getId(), JsonPathExtraction.class);
			Map<String, Extraction> regexExtraction = this.getExtractions(continuITyModel.getApplication(), allExtractedInputs, endpoint.getId(), RegExExtraction.class);
				
			String httpOperationName = serviceName;
			String httpEndpoint = endpointUrl == null ? endpoint.getPath() : endpointUrl;
			Method httpMethod = Method.valueOf(endpoint.getMethod());
			Protocol httpProtocol = Protocol.valueOf(endpoint.getProtocol().toUpperCase());
			ThinkTime calculatedThinkTime = thinkTimeHelper.getCalculatedThinkTimeOfState(serviceName);
			
			Map<String, String> headers = this.getHeaders(endpoint);

			Operation operation = new Operation(
					httpOperationName,
					httpProtocol, 
					httpEndpoint, 
					httpMethod, 
					ScalaHelper.mapAsOptionScalaMap(headers), 
					Option.apply(calculatedThinkTime),
					Option.apply(parameterBundle.getBodyInput()),
					ScalaHelper.mapAsOptionScalaMap(parameterBundle.getQueryParameter()),
					ScalaHelper.mapAsOptionScalaMap(parameterBundle.getUrlParameter()),
					ScalaHelper.mapAsOptionScalaMap(regexExtraction),
					ScalaHelper.mapAsOptionScalaMap(jsonExtraction));
			
			operations.add(operation);
		}

		return operations;
	}
	
	/**
	 * Returns a bundle with all parameters.
	 * 
	 * @param application
	 * @param endpointAnnotation
	 * @return
	 */
	private HttpParameterBundle extractParameters(Application application, EndpointAnnotation endpointAnnotation) {
	
		HttpParameterBundle parameterBundle = new HttpParameterBundle();
		Map<String, Parameter> bodyForm = new HashMap<String, Parameter>();

		for(ParameterAnnotation parameterAnnotation : endpointAnnotation.getParameterAnnotations()) {
			parameterAnnotation.getAnnotatedParameter().resolve(application);
			if(!parameterAnnotation.getAnnotatedParameter().isResolved()) {
				String exceptionMessage = String.format("Exception during resolving parameter annotation with id '%s' using referred id '%s'!", 
						parameterAnnotation.getId(), parameterAnnotation.getAnnotatedParameter().getId());
				LOGGER.error(exceptionMessage);
				throw new IllegalArgumentException(exceptionMessage);
			}
			HttpParameter referredParameter = (HttpParameter) parameterAnnotation.getAnnotatedParameter().getReferred();						
			
			if(parameterAnnotation.getInput() instanceof DirectListInput || parameterAnnotation.getInput() instanceof ExtractedInput) {
				
				Parameter parameter = null;
				
				if(parameterAnnotation.getInput() instanceof DirectListInput) {
					DirectListInput directInput = (DirectListInput) parameterAnnotation.getInput();				
					parameter = getParameterFromDirectListInput(directInput);
					
				} else {

					ExtractedInput extractedInput = (ExtractedInput) parameterAnnotation.getInput();									
					String extractedInputId = extractedInput.getId();
					parameter = new Parameter("${" + extractedInputId + "}");
					
				}
				
				switch(referredParameter.getParameterType()) {
				case REQ_PARAM:
					parameterBundle.getQueryParameter().put(referredParameter.getName(), parameter);
					break;
				case URL_PART:
					parameterBundle.getUrlParameter().put(referredParameter.getName(), parameter);
					break;
				case FORM:	
					bodyForm.put(referredParameter.getName(), parameter);
					break;
				case BODY:
					parameterBundle.setBodyInput(new Body(parameter));
					break;
				default:
					break;
				}
			} else if(parameterAnnotation.getInput() instanceof CsvInput) {
				
				CsvInput csvInput = (CsvInput) parameterAnnotation.getInput();
				// TODO: Add CSV transformation
			}
		}	
		
		if(!bodyForm.isEmpty()) {
			parameterBundle.setBodyInput(new BodyForm(ScalaHelper.mapAsScalaMap(bodyForm)));
		}
		
		return parameterBundle;
	}
	
	private Parameter getParameterFromDirectListInput(DirectListInput directInput) {
		Iterator<String> iterator = directInput.getData().iterator();
		scala.collection.immutable.List<String> convertedValue = JavaConverters.asScalaIteratorConverter(iterator).asScala().toList();
		return new Parameter(convertedValue, Option.empty());	
	}
	
	/**
	 * Returns a map of all headers from the provided endpoint.
	 * 
	 * @param endpoint
	 * 					Endpoint for extraction.
	 * @return Map of headers.
	 */
	private Map<String, String> getHeaders(HttpEndpoint endpoint) {
		
		if(endpoint.getHeaders().isEmpty()) {
			return null;
		}
		
		Map<String, String> headers = new HashMap<String, String>();
		for(String header : endpoint.getHeaders()) {
			String[] headerParts = header.split(":");
			headers.put(headerParts[0].trim(), headerParts[1].trim());
		}	

		return headers;
	}
	
	/**
	 * Returns a map of all regex extractions which are matching to the provided endpointId.
	 * 
	 * @param application
	 * 						Application model.
	 * @param extractedInputs
	 * 						List of all regex extractions.
	 * @param endpointId
	 * 						Endpoint id for which regex extractions are searched.
	 * @return Map of matched regex extractions.
	 */
	private Map<String, Extraction> getExtractions(Application application, List<ExtractedInput> extractedInputs, String endpointId, Class<? extends ValueExtraction> valueExtractionType) {

		Map<String, Extraction> regexExtraction = new HashMap<String, Extraction>();
			
		for(ExtractedInput extracted : extractedInputs) {
			for(ValueExtraction extraction : extracted.getExtractions().stream()
					.filter(e -> (endpointId.equals(e.getFrom().getId()) && valueExtractionType.isInstance(e)))
					.collect(Collectors.toList())) {
				
				extraction.getFrom().resolve(application);
				if(!extraction.getFrom().isResolved()) {
					String exceptionMessage = String.format("Extraction-from is not resolved! (Referred id: '%s')", extraction.getFrom().getId());
					LOGGER.error(exceptionMessage);
					throw new IllegalArgumentException(exceptionMessage);
				}

				HttpEndpoint regexEndpoint = (HttpEndpoint)extraction.getFrom().getReferred();
				String regexEndpointId = regexEndpoint.getId();

				if(regexEndpointId.equals(endpointId)) {
					String pattern = "";
					if(extraction instanceof RegExExtraction) {
						pattern = ((RegExExtraction) extraction).getPattern();
					} else if(extraction instanceof JsonPathExtraction) {
						pattern = ((JsonPathExtraction) extraction).getJsonPath();
					} else {
						String exceptionMessage = String.format("Extraction type is not defined! (Type: '%s')", valueExtractionType.getName());
						LOGGER.error(exceptionMessage);
						throw new IllegalArgumentException(exceptionMessage);
					}
					Object matchNumber = extraction.getMatchNumber();
					String defaultValue = extraction.getFallbackValue();
					Extraction benchFlowExtraction = new Extraction(pattern, Option.apply(defaultValue), Option.apply(matchNumber));
					regexExtraction.put(extracted.getId(), benchFlowExtraction);
				}
			}
		}
		
		return regexExtraction;
	}
}
