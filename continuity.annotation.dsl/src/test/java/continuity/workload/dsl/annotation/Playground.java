package continuity.workload.dsl.annotation;

import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.ServiceInterface;

/**
 * @author Henning Schulz
 *
 */
public class Playground {

	public static void main(String[] args) throws ClassNotFoundException {
		ServiceInterface<?> interf = new HttpInterface();
		interf.setId("id");
		WeakReference<ServiceInterface<?>> ref = WeakReference.create(interf);

		HttpParameter param = new HttpParameter();
		param.setId("id");
		ServiceInterface<?> resolved = ref.resolve(param);

		System.out.println(resolved);
	}

}
