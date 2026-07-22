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

import java.util.Comparator;
import java.util.Set;

import com.pembana.raingauge.station.client.IemStationVariableClient;
import com.pembana.raingauge.station.client.ProviderException;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Provides rainfall capability operations.
 * @author Gunnar Hillert
 */
@Service
public class RainfallCapabilityService {

	private final IemStationVariableClient stationVariableClient;

	/**
	 * Creates a new {@code RainfallCapabilityService}.
	 * @param stationVariableClient the station variable client
	 */
	public RainfallCapabilityService(IemStationVariableClient stationVariableClient) {
		this.stationVariableClient = stationVariableClient;
	}

	/**
	 * Discovers a station's rainfall capability from provider variables.
	 * @param station the station to process
	 * @return the resulting discover
	 */
	public CapabilityDiscovery discover(Station station) {
		if (station.getPrecipitationKey() != null) {
			return new CapabilityDiscovery(station.getRainfallCapability(), station.getPrecipitationKey());
		}
		try {
			Set<String> variables = this.stationVariableClient.fetchRecentVariables(station.getStationId());
			String accumulator = variables.stream()
					.filter((variable) -> variable.startsWith("PC"))
					.min(Comparator.naturalOrder())
					.orElse(null);
			if (accumulator != null) {
				return new CapabilityDiscovery(RainfallCapability.SUPPORTED_ACCUMULATOR,
						normalize(accumulator));
			}
			String interval = variables.stream()
					.filter((variable) -> variable.startsWith("PP"))
					.min(Comparator.naturalOrder())
					.orElse(null);
			if (interval != null) {
				return new CapabilityDiscovery(RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION,
						normalize(interval));
			}
			return new CapabilityDiscovery(RainfallCapability.TEMPORARILY_SILENT, null);
		} catch (ProviderException ex) {
			return new CapabilityDiscovery(RainfallCapability.PRECIPITATION_TYPE_UNKNOWN, null);
		}
	}

	/**
	 * Normalizes a provider variable key for comparison.
	 * @param key the key
	 * @return the resulting normalize
	 */
	private String normalize(String key) {
		return key.endsWith("ZZ") ? key.substring(0, key.length() - 2) : key;
	}

	/**
	 * Describes a capability discovery.
	 * @param capability the capability
	 * @param precipitationKey the precipitation key
	 * @author Gunnar Hillert
	 */
	public record CapabilityDiscovery(RainfallCapability capability,
			@Nullable String precipitationKey) {
	}

}
