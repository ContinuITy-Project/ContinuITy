package org.continuity.cli.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.continuity.api.entities.order.Order;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.OrderResponse;
import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.idpa.AppId;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderStorage {

	public static final String ID_LATEST = "LATEST";

	private static final String FILENAME_ORDER = "order.yml";

	private static final String FILENAME_RESPONSE = "response.yml";

	private static final String FILENAME_REPORT = "report.yml";

	private static final String FILENAME_NEW = "order-new.yml";

	private final OrderDirectoryManager directory;

	private final ObjectMapper mapper;

	public OrderStorage(PropertiesProvider properties, ObjectMapper mapper) {
		this.directory = new OrderDirectoryManager("order", properties);
		this.mapper = mapper;
	}

	public Path storeAsNew(AppId aid, Order order) throws IOException {
		Path path = directory.getDir(aid, true).resolve(FILENAME_NEW);
		mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), order);
		return path;
	}

	public Order readNew(AppId aid) throws IOException {
		File file = directory.getDir(aid, false).resolve(FILENAME_NEW).toFile();

		if (file.exists()) {
			return mapper.readValue(file, Order.class);
		} else {
			return null;
		}
	}

	/**
	 * Moves the new order to its appropriate folder. Assumes that the corresponding response has
	 * already been stored using {@link #store(AppId, OrderResponse)}.
	 *
	 * @param aid
	 *            The app-id.
	 * @param orderId
	 *            The order-id.
	 * @throws IOException
	 */
	public void moveNew(AppId aid, String orderId) throws IOException {
		Path rootDir = directory.getDir(aid, false);
		Path dir = directory.getDir(aid, orderId, true);
		Files.move(rootDir.resolve(FILENAME_NEW), dir.resolve(FILENAME_ORDER));
	}

	public Order readOrder(AppId aid, String orderId) throws IOException {
		if (ID_LATEST.equals(orderId)) {
			return readLatestOrder(aid);
		}

		Path path = directory.getDir(aid, orderId, false).resolve(FILENAME_ORDER);

		if (path.toFile().exists()) {
			return mapper.readValue(path.toFile(), Order.class);
		} else {
			return null;
		}
	}

	public Order readLatestOrder(AppId aid) throws IOException {
		Path dir = directory.getLatest(aid);

		if (dir != null) {
			return readOrder(aid, dir.getFileName().toString());
		} else {
			return null;
		}
	}

	public String store(AppId aid, OrderResponse response) throws IOException {
		String orderId = RestApi.Orchestrator.Orchestration.WAIT.parsePathParameters(response.getWaitLink()).get(0);
		Path dir = directory.getFreshDir(aid, orderId);

		mapper.writeValue(dir.resolve(FILENAME_RESPONSE).toFile(), response);

		return orderId;
	}

	public OrderResponse readResponse(AppId aid, String orderId) throws IOException {
		if (ID_LATEST.equals(orderId)) {
			return readLatestResponse(aid);
		}

		Path path = directory.getDir(aid, orderId, false).resolve(FILENAME_RESPONSE);

		if (path.toFile().exists()) {
			return mapper.readValue(path.toFile(), OrderResponse.class);
		} else {
			return null;
		}
	}

	public OrderResponse readLatestResponse(AppId aid) throws IOException {
		Path dir = directory.getLatest(aid);

		if (dir != null) {
			return readResponse(aid, dir.getFileName().toString());
		} else {
			return null;
		}
	}

	public void store(AppId aid, OrderReport report) throws IOException {
		String orderId = report.getOrderId();
		Path dir = directory.getDir(aid, orderId, true);

		mapper.writeValue(dir.resolve(FILENAME_REPORT).toFile(), report);
	}

	public OrderReport readReport(AppId aid, String orderId) throws IOException {
		if (ID_LATEST.equals(orderId)) {
			return readLatestReport(aid);
		}

		Path path = directory.getDir(aid, orderId, false).resolve(FILENAME_REPORT);

		if (path.toFile().exists()) {
			return mapper.readValue(path.toFile(), OrderReport.class);
		} else {
			return null;
		}
	}

	public OrderReport readLatestReport(AppId aid) throws IOException {
		Path dir = directory.getLatest(aid);

		if (dir != null) {
			return readReport(aid, dir.getFileName().toString());
		} else {
			return null;
		}
	}

	public int clean(AppId aid, boolean includeCurrent) {
		int num = directory.clearArchive(aid);

		if (includeCurrent) {
			num += directory.clearCurrent(aid);
		}

		return num;
	}

}
