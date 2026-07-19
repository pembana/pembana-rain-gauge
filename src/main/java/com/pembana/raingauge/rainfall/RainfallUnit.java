package com.pembana.raingauge.rainfall;

public enum RainfallUnit {

	IMPERIAL("imperial", "in"),

	METRIC("metric", "mm");

	private final String token;

	private final String symbol;

	RainfallUnit(String token, String symbol) {
		this.token = token;
		this.symbol = symbol;
	}

	public String token() {
		return this.token;
	}

	public String symbol() {
		return this.symbol;
	}

	public static RainfallUnit fromToken(String token) {
		for (RainfallUnit unit : values()) {
			if (unit.token.equalsIgnoreCase(token)) {
				return unit;
			}
		}
		throw new IllegalArgumentException("Unsupported rainfall unit: " + token);
	}

}
