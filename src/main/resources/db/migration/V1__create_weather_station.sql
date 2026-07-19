CREATE TABLE weather_station (
    id UUID NOT NULL,
    network VARCHAR(32) NOT NULL,
    station_id VARCHAR(32) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    alias VARCHAR(32),
    island VARCHAR(64),
    region VARCHAR(128),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(10, 6),
    elevation NUMERIC(10, 3),
    archive_begin DATE,
    archive_end DATE,
    source_online BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    featured BOOLEAN NOT NULL,
    catalog_confirmed BOOLEAN NOT NULL,
    rainfall_capability VARCHAR(48) NOT NULL,
    precipitation_key VARCHAR(16),
    state VARCHAR(8),
    country VARCHAR(8),
    time_zone VARCHAR(64),
    disabled_reason VARCHAR(255),
    override_note VARCHAR(500),
    source_metadata VARCHAR(4000),
    latest_observation_at TIMESTAMP WITH TIME ZONE,
    catalog_last_seen_at TIMESTAMP WITH TIME ZONE,
    catalog_refreshed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_weather_station PRIMARY KEY (id),
    CONSTRAINT uk_weather_station_network_station_id UNIQUE (network, station_id)
);

CREATE INDEX ix_weather_station_public_name
    ON weather_station (enabled, display_name);

CREATE INDEX ix_weather_station_station_id
    ON weather_station (station_id);
