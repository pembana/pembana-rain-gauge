package com.pembana.raingauge.station;

import java.util.Comparator;
import java.util.Set;

import com.pembana.raingauge.station.client.IemStationVariableClient;
import com.pembana.raingauge.station.client.ProviderException;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class RainfallCapabilityService {

	private final IemStationVariableClient stationVariableClient;

	public RainfallCapabilityService(IemStationVariableClient stationVariableClient) {
		this.stationVariableClient = stationVariableClient;
	}

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

	private String normalize(String key) {
		return key.endsWith("ZZ") ? key.substring(0, key.length() - 2) : key;
	}

	public record CapabilityDiscovery(RainfallCapability capability,
			@Nullable String precipitationKey) {
	}

}
