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

package com.pembana.raingauge.station;

import java.util.regex.Pattern;

/**
 * Identifies how a station measures and reports precipitation.
 * @author OpenAI Codex
 */
public enum StationType {

	/** Automated station with native accumulator or interval observations. */
	AUTOMATED("[A-Z0-9]{3,12}"),

	/** Volunteer station with daily and multi-day precipitation reports. */
	DAILY_REPORTS("HI-[A-Z]{2}-[0-9]{1,6}");

	/** IEM metadata network for Hawaiʻi CoCoRaHS stations. */
	public static final String COCORAHS_NETWORK = "HI_COCORAHS";

	private final Pattern stationIdPattern;

	StationType(String stationIdPattern) {
		this.stationIdPattern = Pattern.compile(stationIdPattern, Pattern.CASE_INSENSITIVE);
	}

	/**
	 * Derives station type from the catalog network.
	 * @param network the provider network
	 * @return the station type
	 */
	public static StationType fromNetwork(String network) {
		return COCORAHS_NETWORK.equals(network) ? DAILY_REPORTS : AUTOMATED;
	}

	/**
	 * Tests whether an identifier uses the format for this station type.
	 * @param stationId the station identifier
	 * @return whether the identifier matches
	 */
	public boolean acceptsStationId(String stationId) {
		return this.stationIdPattern.matcher(stationId).matches();
	}

}
