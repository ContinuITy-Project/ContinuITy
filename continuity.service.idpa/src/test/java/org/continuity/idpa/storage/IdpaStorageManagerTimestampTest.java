package org.continuity.idpa.storage;

import static org.continuity.idpa.StaticIdpaTestInstance.FIRST;
import static org.continuity.idpa.StaticIdpaTestInstance.FIRST_REFINED;
import static org.continuity.idpa.StaticIdpaTestInstance.SECOND;
import static org.continuity.idpa.StaticIdpaTestInstance.THIRD;

import org.apache.commons.lang.time.DateUtils;
import org.continuity.idpa.StaticIdpaTestInstance;
import org.continuity.idpa.VersionOrTimestamp;

public class IdpaStorageManagerTimestampTest extends IdpaStorageManagerTest {

	@Override
	protected StaticIdpaTestInstance first() {
		return FIRST;
	}

	@Override
	protected StaticIdpaTestInstance first_refined() {
		return FIRST_REFINED;
	}

	@Override
	protected StaticIdpaTestInstance second() {
		return SECOND;
	}

	@Override
	protected StaticIdpaTestInstance third() {
		return THIRD;
	}

	@Override
	protected VersionOrTimestamp addSmallOffset(VersionOrTimestamp version) {
		return new VersionOrTimestamp(DateUtils.addSeconds(version.getTimestamp(), 10));
	}

}
