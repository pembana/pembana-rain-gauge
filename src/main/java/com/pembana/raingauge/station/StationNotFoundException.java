package com.pembana.raingauge.station;

public class StationNotFoundException extends RuntimeException {

	public StationNotFoundException(String stationId) {
		super("No enabled station was found for ID " + stationId);
	}

}
