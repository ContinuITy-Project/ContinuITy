package org.continuity.orchestrator.entities;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderReportCounter {

	private final String orderId;

	private final int numReports;

	private AtomicInteger numReturned = new AtomicInteger(0);

	public OrderReportCounter(String orderId, int numReports) {
		this.orderId = orderId;
		this.numReports = numReports;
	}

	public String getOrderId() {
		return orderId;
	}

	public int getNumReports() {
		return numReports;
	}

	public int nextReportNumber() {
		int num = numReturned.incrementAndGet();

		if (num > numReports) {
			return -1;
		} else {
			return num;
		}
	}

}
