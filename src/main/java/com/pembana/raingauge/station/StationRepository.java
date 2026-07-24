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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence operations for station.
 * @author Gunnar Hillert
 */
public interface StationRepository extends JpaRepository<Station, UUID> {

	/**
	 * Finds by network and station ID.
	 * @param network the provider network identifier
	 * @param stationId the provider station identifier
	 * @return the matching by network and station ID
	 */
	Optional<Station> findByNetworkAndStationId(String network, String stationId);

	/**
	 * Finds by station ID ignore case.
	 * @param stationId the provider station identifier
	 * @return the matching by station ID ignore case
	 */
	Optional<Station> findByStationIdIgnoreCase(String stationId);

	/**
	 * Finds all by enabled true order by display name asc.
	 * @return the matching all by enabled true order by display name asc
	 */
	List<Station> findAllByEnabledTrueOrderByDisplayNameAsc();

	/**
	 * Finds rainfall stations.
	 * @param capabilities the supported capabilities
	 * @return the matching rainfall stations
	 */
	@Query("""
			select station from Station station
			where station.enabled = true
			and station.rainfallCapability in :capabilities
			and station.precipitationKey is not null
			order by station.displayName
			""")
	List<Station> findRainfallStations(
			@Param("capabilities") Set<RainfallCapability> capabilities);

	/**
	 * Finds all by featured true and enabled true order by display name asc.
	 * @return the matching all by featured true and enabled true order by display name asc
	 */
	List<Station> findAllByFeaturedTrueAndEnabledTrueOrderByDisplayNameAsc();

	/**
	 * Finds all by order by display name asc.
	 * @return the matching all by order by display name asc
	 */
	List<Station> findAllByOrderByDisplayNameAsc();

}
