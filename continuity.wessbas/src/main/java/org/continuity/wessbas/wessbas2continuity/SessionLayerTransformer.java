package org.continuity.wessbas.wessbas2continuity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.continuity.workload.dsl.system.ServiceInterface;

import m4jdsl.ApplicationModel;
import m4jdsl.ApplicationState;
import m4jdsl.ProtocolState;
import m4jdsl.Request;

/**
 * Extracts the single requests of an {@link ApplicationModel} of the WESSBAS DSL and transforms
 * them to {@link ServiceInterface}s. Each time one interface has been found, registered listeners
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

	private final ApplicationModel applicationModel;

	private List<Consumer<ServiceInterface<?>>> interfaceListeners;

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
	 * Registers a new listener that is called when a new {@link ServiceInterface} has been found.
	 *
	 * @param listener
	 *            The listener to be called.
	 */
	public void registerOnInterfaceFoundListener(Consumer<ServiceInterface<?>> listener) {
		if (interfaceListeners == null) {
			interfaceListeners = new ArrayList<>();
		}

		interfaceListeners.add(listener);
	}

	private void onInterfaceFound(ServiceInterface<?> sInterface) {
		interfaceListeners.forEach(l -> l.accept(sInterface));
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
		int numProtocolStates = state.getProtocolDetails().getProtocolStates().size();

		if (numProtocolStates != 1) {
			throw new IllegalStateException("Application state " + interfaceName + " has " + numProtocolStates + " protocol states. Expected one!");
		}

		ProtocolState protocolState = state.getProtocolDetails().getProtocolStates().get(0);
		Request request = protocolState.getRequest();

		ServiceInterface<?> interf = RequestTransformer.get(request.getClass()).transform(request);
		interf.setId(interfaceName);

		onInterfaceFound(interf);
	}

}
