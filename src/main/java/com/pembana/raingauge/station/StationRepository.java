package com.pembana.raingauge.station;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, UUID> {

	Optional<Station> findByNetworkAndStationId(String network, String stationId);

	Optional<Station> findByStationIdIgnoreCase(String stationId);

	List<Station> findAllByEnabledTrueOrderByDisplayNameAsc();

	List<Station> findAllByFeaturedTrueAndEnabledTrueOrderByDisplayNameAsc();

	List<Station> findAllByOrderByDisplayNameAsc();

}
