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
 * Signals unsupported rainfall station.
 * @author Gunnar Hillert
 */
public class UnsupportedRainfallStationException extends RuntimeException {

	/**
	 * Creates a new {@code UnsupportedRainfallStationException}.
	 * @param stationId the provider station identifier
	 */
	public UnsupportedRainfallStationException(String stationId) {
		super("Station " + stationId + " does not currently have a supported accumulator variable");
	}

}
