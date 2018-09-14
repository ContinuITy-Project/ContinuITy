package org.continuity.request.rates.controllers;

import static org.continuity.api.rest.RestApi.RequestRates.RequestLogs.ROOT;
import static org.continuity.api.rest.RestApi.RequestRates.RequestLogs.Paths.GET;
import static org.continuity.api.rest.RestApi.RequestRates.RequestLogs.Paths.UPLOAD;

import java.net.URI;
import java.util.List;

import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.CsvFileStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.request.rates.entities.CsvRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ROOT)
public class RequestLogsController {

	private static final String TAG = "requests";

	@Autowired
	private CsvFileStorage<CsvRow> storage;

	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<String> getRequestLogs(@PathVariable String id) {
		List<CsvRow> requestLogs = storage.get(id);

		if (requestLogs == null) {
			return ResponseEntity.notFound().build();
		} else {
			String logsAsString = requestLogs.stream().map(CsvRow::toString).reduce((a, b) -> a + "\n" + b).get();
			return ResponseEntity.ok(logsAsString);
		}
	}

	@RequestMapping(path = UPLOAD, method = RequestMethod.POST)
	public ResponseEntity<String> uploadRequestLogs(@RequestBody String requestLogs) {
		if (requestLogs == null) {
			return ResponseEntity.badRequest().body("Missing body.");
		}

		List<CsvRow> rows = CsvRow.listFromString(requestLogs);

		boolean parseable = rows.stream().map(CsvRow::checkDates).reduce(Boolean::logicalAnd).get();

		if (parseable) {
			String id = storage.put(rows, TAG);
			String link = RestApi.RequestRates.RequestLogs.GET.requestUrl(id).withoutProtocol().get();
			return ResponseEntity.created(URI.create(WebUtils.addProtocolIfMissing(link))).body(link);
		} else {
			return ResponseEntity.badRequest().body("Illegal date format!");
		}
	}

}
