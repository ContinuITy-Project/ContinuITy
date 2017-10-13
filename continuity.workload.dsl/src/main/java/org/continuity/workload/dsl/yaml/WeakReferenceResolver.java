package org.continuity.workload.dsl.yaml;

import java.util.Collection;
import java.util.Collections;

import org.continuity.utils.enums.EnumForClassHolder;
import org.continuity.workload.dsl.ContinuityModelElement;
import org.continuity.workload.dsl.WeakReference;
import org.continuity.workload.dsl.annotation.InterfaceAnnotation;
import org.continuity.workload.dsl.annotation.RegExExtraction;
import org.continuity.workload.dsl.system.ServiceInterface;

/**
 * @author Henning Schulz
 *
 */
public enum WeakReferenceResolver {

	INTERFACE_ANNOTATION(InterfaceAnnotation.class) {
		@Override
		public boolean isWeakReference(String key) {
			return "interface".equals(key);
		}

		@Override
		public void resolveWeakReference(String key, String value, ContinuityModelElement element) {
			InterfaceAnnotation ann = (InterfaceAnnotation) element;
			@SuppressWarnings("unchecked")
			Class<ServiceInterface<?>> clazz = (Class<ServiceInterface<?>>) (Class<?>) ServiceInterface.class;
			ann.setAnnotatedInterface(WeakReference.create(clazz, value));
		}
	},

	REG_EX_EXTRACTION(RegExExtraction.class) {
		@Override
		public boolean isWeakReference(String key) {
			return "extracted".equals(key);
		}

		@Override
		public void resolveWeakReference(String key, String value, ContinuityModelElement element) {
			RegExExtraction extraction = (RegExExtraction) element;
			@SuppressWarnings("unchecked")
			Class<ServiceInterface<?>> clazz = (Class<ServiceInterface<?>>) (Class<?>) ServiceInterface.class;
			extraction.setExtracted(WeakReference.create(clazz, value));
		}
	},

	NOOP(null) {
		@Override
		public boolean isWeakReference(String key) {
			return false;
		}

		@Override
		public void resolveWeakReference(String key, String value, ContinuityModelElement element) {
			// do nothing
		}
	};

	private final Class<? extends ContinuityModelElement> type;

	private static final EnumForClassHolder<WeakReferenceResolver, ContinuityModelElement> enumHolder = new EnumForClassHolder<WeakReferenceResolver, ContinuityModelElement>(
			WeakReferenceResolver.class, WeakReferenceResolver::getTypes, NOOP);

	/**
	 *
	 */
	private WeakReferenceResolver(Class<? extends ContinuityModelElement> type) {
		this.type = type;
	}

	public abstract boolean isWeakReference(String key);

	public abstract void resolveWeakReference(String key, String value, ContinuityModelElement element);

	public static WeakReferenceResolver get(Class<? extends ContinuityModelElement> type) {
		return enumHolder.get(type);
	}

	private Collection<Class<? extends ContinuityModelElement>> getTypes() {
		return Collections.singletonList(type);
	}

}
