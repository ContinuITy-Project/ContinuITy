package org.continuity.wessbas.model.stubs;

import org.continuity.wessbas.model.ModelGeneratorTestConfig;
import org.emfjson.jackson.module.EMFModule;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.TestComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
@TestComponent
public class ModelCreatedHandlingStub {

	private final ObjectMapper jsonMapper = EMFModule.setupDefaultMapper();

	private WorkloadModel receivedWorkloadModel;

	@RabbitListener(queues = ModelGeneratorTestConfig.MODEL_CREATED_QUEUE_NAME)
	public void handleModelCreated(Object workloadModelJson) {
		System.out.println("Received message of type " + workloadModelJson.getClass() + " : " + workloadModelJson);

		// ObjectMapper mapper = new ObjectMapper();
		//
		// EMFModule module = new EMFModule();
		// module.configure(EMFModule.Feature.OPTION_SERIALIZE_TYPE, false);
		// mapper.registerModule(module);
		//
		// M4jdslPackageImpl.init();
		// M4jdslPackage.eINSTANCE.eClass();
		// System.out.println("m4jdsl package: " + M4jdslPackage.eINSTANCE.eResource());
		//
		// ResourceSetImpl resourceSet = new ResourceSetImpl();
		// resourceSet.getResourceFactoryRegistry()
		// .getExtensionToFactoryMap()
		// .put("*", new JsonResourceFactory(mapper));
		//
		// WorkloadModel workloadModel;
		// // try {
		// // workloadModel = jsonMapper.treeToValue(workloadModelJson.get("workload-model"),
		// // WorkloadModel.class);
		// //
		// // } catch (JsonProcessingException e) {
		// // throw new RuntimeException(e);
		// // }
		//
		// try {
		// workloadModel = mapper.reader().withAttribute(EMFContext.Attributes.RESOURCE_SET,
		// resourceSet).withAttribute(EMFContext.Attributes.ROOT_ELEMENT,
		// M4jdslPackage.Literals.WORKLOAD_MODEL)
		// .treeToValue(workloadModelJson.get("workload-model"),
		// WorkloadModel.class);
		// } catch (IOException e) {
		// throw new RuntimeException(e);
		// }
		//
		// receivedWorkloadModel = workloadModel;
		// System.out.println("Stored workload model " + workloadModel);
	}

	/**
	 * Gets {@link #receivedWorkloadModel}.
	 *
	 * @return {@link #receivedWorkloadModel}
	 */
	public WorkloadModel getReceivedWorkloadModel() {
		return this.receivedWorkloadModel;
	}

}
