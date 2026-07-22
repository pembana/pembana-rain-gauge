/*
 * Copyright 2026 Gunnar Hillert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pembana.raingauge.rainfall;

/**
 * Enumerates the supported rainfall unit values.
 * @author Gunnar Hillert
 */
public enum RainfallUnit {

	/** Inches with imperial display precision. */
	IMPERIAL("imperial", "in"),

	/** Millimeters with metric display precision. */
	METRIC("metric", "mm");

	private final String token;

	private final String symbol;

	/**
	 * Creates a new {@code RainfallUnit}.
	 * @param token the token
	 * @param symbol the symbol
	 */
	RainfallUnit(String token, String symbol) {
		this.token = token;
		this.symbol = symbol;
	}

	/**
	 * Returns the external token for this value.
	 * @return the external token for this value
	 */
	public String token() {
		return this.token;
	}

	/**
	 * Returns the display symbol for this rainfall unit.
	 * @return the display symbol for this unit
	 */
	public String symbol() {
		return this.symbol;
	}

	/**
	 * Resolves the value represented by an external token.
	 * @param token the token
	 * @return the value represented by the supplied token or domain object
	 */
	public static RainfallUnit fromToken(String token) {
		for (RainfallUnit unit : values()) {
			if (unit.token.equalsIgnoreCase(token)) {
				return unit;
			}
		}
		throw new IllegalArgumentException("Unsupported rainfall unit: " + token);
	}

}
