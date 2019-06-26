package org.continuity.forecast.test;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;

public class Main {

	public static void main(String[] args) {
		
		InfluxDB influxDB = InfluxDBFactory.connect("http://127.0.0.1:8086", "admin", "admin");
		
		Pong response = influxDB.ping();
		if (response.getVersion().equalsIgnoreCase("unknown")) {
		    System.out.println("Error pinging server.");
		    return;
		} 
	}
}
