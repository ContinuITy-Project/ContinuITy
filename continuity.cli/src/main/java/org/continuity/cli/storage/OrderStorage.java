package org.continuity.cli.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.OrderResponse;
import org.continuity.cli.config.PropertiesProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

public class OrderStorage {

	public static final String ID_LATEST = "LATEST";

	private static final String STORAGE_DIR = "orders";

	private static final String ORDER_FILE = "order.yml";

	private static final String LINKS_FILE = "links.yml";

	private static final String REPORT_FILE = "report.yml";

	@Autowired
	private PropertiesProvider propertiesProvider;

	private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));

	private AtomicInteger counter = new AtomicInteger(1);

	private String latest = null;

	public String newOrder(Order order) throws JsonGenerationException, JsonMappingException, IOException {
		String id = counter.getAndIncrement() + "-" + order.getGoal().toPrettyString() + "-" + order.getTag();

		File orderFile = getOrderDir(id).resolve(ORDER_FILE).toFile();
		mapper.writeValue(orderFile, order);

		latest = id;

		return id;
	}

	public Order getOrder(String id) {
		id = resolveLatest(id);
		File orderFile = getOrderDir(id).resolve(ORDER_FILE).toFile();

		try {
			return mapper.readValue(orderFile, Order.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void storeLinks(String id, OrderResponse links) throws JsonGenerationException, JsonMappingException, IOException {
		id = resolveLatest(id);
		File linksFile = getOrderDir(id).resolve(LINKS_FILE).toFile();

		mapper.writeValue(linksFile, links);
	}

	public OrderResponse getLinks(String id) {
		id = resolveLatest(id);
		File linksFile = getOrderDir(id).resolve(LINKS_FILE).toFile();

		try {
			return mapper.readValue(linksFile, OrderResponse.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void storeReport(String id, OrderReport report) throws JsonGenerationException, JsonMappingException, IOException {
		id = resolveLatest(id);
		File reportFile = getOrderDir(id).resolve(REPORT_FILE).toFile();

		mapper.writeValue(reportFile, report);
	}

	public OrderReport getReport(String id) {
		id = resolveLatest(id);
		File reportFile = getOrderDir(id).resolve(REPORT_FILE).toFile();

		try {
			return mapper.readValue(reportFile, OrderReport.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int clean() throws IOException {
		int removedCount = 0;

		for (File dir : Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR), STORAGE_DIR).toFile().listFiles()) {
			if (dir.isDirectory()) {
				FileUtils.deleteDirectory(dir);
				removedCount++;
			}
		}

		return removedCount;
	}

	private Path getOrderDir(String id) {
		Path orderDir = Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR), STORAGE_DIR, id);
		orderDir.toFile().mkdirs();
		return orderDir;
	}

	private String resolveLatest(String id) {
		if (ID_LATEST.equals(id)) {
			return latest;
		} else {
			return id;
		}
	}

}
