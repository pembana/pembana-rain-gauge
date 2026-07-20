package com.pembana.raingauge.station;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<Station, UUID> {

	Optional<Station> findByNetworkAndStationId(String network, String stationId);

	Optional<Station> findByStationIdIgnoreCase(String stationId);

	List<Station> findAllByEnabledTrueOrderByDisplayNameAsc();

	@Query("""
			select station from Station station
			where station.enabled = true
			and station.rainfallCapability = :capability
			and station.precipitationKey is not null
			order by station.displayName
			""")
	List<Station> findRainfallStations(@Param("capability") RainfallCapability capability);

	List<Station> findAllByFeaturedTrueAndEnabledTrueOrderByDisplayNameAsc();

	List<Station> findAllByOrderByDisplayNameAsc();

}
