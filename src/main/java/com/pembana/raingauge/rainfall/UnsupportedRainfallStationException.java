package com.pembana.raingauge.rainfall;

public class UnsupportedRainfallStationException extends RuntimeException {

	public UnsupportedRainfallStationException(String stationId) {
		super("Station " + stationId + " does not currently have a supported accumulator variable");
	}

}
