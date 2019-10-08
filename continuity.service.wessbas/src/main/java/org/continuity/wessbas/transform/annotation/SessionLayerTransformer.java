package org.continuity.wessbas.transform.annotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.math3.util.Pair;
import org.continuity.commons.utils.StringUtils;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Endpoint;

import m4jdsl.ApplicationModel;
import m4jdsl.ApplicationState;
import m4jdsl.ProtocolState;
import m4jdsl.Request;

/**
 * Extracts the single requests of an {@link ApplicationModel} of the WESSBAS DSL and transforms
 * them to {@link Endpoint}s. Each time one interface has been found, registered listeners
 * are called. The listeners can be registered via
 * {@link SessionLayerTransformer#registerOnInterfaceFoundListener(Consumer)
 * registerOnInterfaceFoundListener(Consumer)}. <br>
 *
 * Each state of the WESSBAS application model is assumed to hold exactly one protocol state.
 *
 * @author Henning Schulz
 *
 */
public class SessionLayerTransformer {

	private static final String KEY_INITIAL = "INITIAL";

	private final ApplicationModel applicationModel;

	private List<Consumer<Endpoint<?>>> interfaceListeners;

	private List<Consumer<Pair<Input, Parameter>>> inputListeners;

	private final Set<String> usedIds = new HashSet<>();

	/**
	 * Creates a new SessionLayerTransformer for the specified {@link ApplicationModel}.
	 *
	 * @param applicationModel
	 *            The model to be transformed.
	 */
	public SessionLayerTransformer(ApplicationModel applicationModel) {
		this.applicationModel = applicationModel;
	}

	/**
	 * Registers a new listener that is called when a new {@link Endpoint} has been found.
	 *
	 * @param listener
	 *            The listener to be called.
	 */
	public void registerOnInterfaceFoundListener(Consumer<Endpoint<?>> listener) {
		if (interfaceListeners == null) {
			interfaceListeners = new ArrayList<>();
		}

		interfaceListeners.add(listener);
	}

	/**
	 * Registers a new listener that is called when a new {@link Input} has been found.
	 *
	 * @param listener
	 *            The listener to be called.
	 */
	public void registerOnInputFoundListener(Consumer<Pair<Input, Parameter>> listener) {
		if (inputListeners == null) {
			inputListeners = new ArrayList<>();
		}

		inputListeners.add(listener);
	}

	private void onInterfaceFound(Endpoint<?> sInterface) {
		interfaceListeners.forEach(l -> l.accept(sInterface));
	}

	private void onInputFound(Pair<Input, Parameter> inputParamPair) {
		inputListeners.forEach(l -> l.accept(inputParamPair));
	}

	/**
	 * Executes the transformation causing the calls to the listeners that are registered via
	 * {@link SessionLayerTransformer#registerOnInterfaceFoundListener(Consumer)
	 * registerOnInterfaceFoundListener(Consumer)}.
	 *
	 */
	public void transform() {
		for (ApplicationState state : applicationModel.getSessionLayerEFSM().getApplicationStates()) {
			visitApplicationState(state);
		}
	}

	private void visitApplicationState(ApplicationState state) {
		String interfaceName = state.getService().getName();

		if (KEY_INITIAL.equals(interfaceName)) {
			return;
		}

		int numProtocolStates = state.getProtocolDetails().getProtocolStates().size();

		if (numProtocolStates != 1) {
			throw new IllegalStateException("Application state " + interfaceName + " has " + numProtocolStates + " protocol states. Expected one!");
		}

		ProtocolState protocolState = state.getProtocolDetails().getProtocolStates().get(0);
		Request request = protocolState.getRequest();

		Endpoint<?> interf = RequestTransformer.get(request.getClass()).transform(request);
		interf.setId(interfaceName);

		for (Parameter param : interf.getParameters()) {
			HttpParameter httpParam = (HttpParameter) param;
			param.setId(getNewParameterId(interfaceName, httpParam));
		}

		onInterfaceFound(interf);

		List<Pair<Input, Parameter>> inputs = new InputDataTransformer().transform(request, interf);
		inputs.forEach(this::onInputFound);
	}

	private String getNewParameterId(String interfaceName, HttpParameter httpParam) {
		String id = StringUtils.formatAsId(true, interfaceName, httpParam.getName(), httpParam.getParameterType().toString());

		String currId = id;
		int i = 1;

		while (usedIds.contains(currId)) {
			currId = id + "_" + ++i;
		}

		usedIds.add(currId);

		return currId;
	}

}
