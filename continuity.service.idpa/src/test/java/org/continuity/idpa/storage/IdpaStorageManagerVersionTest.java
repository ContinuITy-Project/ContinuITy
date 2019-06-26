package org.continuity.idpa.storage;

import static org.continuity.idpa.StaticIdpaTestInstance.FIRST;
import static org.continuity.idpa.StaticIdpaTestInstance.FIRST_REFINED;
import static org.continuity.idpa.StaticIdpaTestInstance.SECOND;
import static org.continuity.idpa.StaticIdpaTestInstance.THIRD;

import org.continuity.idpa.DynamicIdpaTestInstance;
import org.continuity.idpa.IdpaTestInstance;
import org.continuity.idpa.Version;
import org.continuity.idpa.VersionOrTimestamp;

public class IdpaStorageManagerVersionTest extends IdpaStorageManagerTest {

	@Override
	protected IdpaTestInstance first() {
		IdpaTestInstance inst = new DynamicIdpaTestInstance(FIRST);
		Version version = Version.fromString("v1");
		inst.getApplication().setVersion(version);
		inst.getAnnotation().setVersion(version);
		return inst;
	}

	@Override
	protected IdpaTestInstance first_refined() {
		IdpaTestInstance inst = new DynamicIdpaTestInstance(FIRST_REFINED);
		Version version = Version.fromString("v1.1");
		inst.getApplication().setVersion(version);
		inst.getAnnotation().setVersion(version);
		return inst;
	}

	@Override
	protected IdpaTestInstance second() {
		IdpaTestInstance inst = new DynamicIdpaTestInstance(SECOND);
		Version version = Version.fromString("v2.0.0");
		inst.getApplication().setVersion(version);
		inst.getAnnotation().setVersion(version);
		return inst;
	}

	@Override
	protected IdpaTestInstance third() {
		IdpaTestInstance inst = new DynamicIdpaTestInstance(THIRD);
		Version version = Version.fromString("v3.0");
		inst.getApplication().setVersion(version);
		inst.getAnnotation().setVersion(version);
		return inst;
	}

	@Override
	protected VersionOrTimestamp addSmallOffset(VersionOrTimestamp version) {
		return new VersionOrTimestamp(version.getVersion().increase(2, 1));
	}

}
