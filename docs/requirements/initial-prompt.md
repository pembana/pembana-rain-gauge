# Build Pembana Rain Gauge

You are a senior Java and Spring engineer. Generate a complete, production-quality Maven repository for an application named **Pembana Rain Gauge**.

The application retrieves near-real-time and historical precipitation observations for Hawaiʻi weather stations, calculates rainfall totals from native station observations as accurately as possible, and presents the results through modern server-rendered JTE pages enhanced with AJAX and Apache ECharts.

The finished application should be a substantially more modern, responsive, and usable alternative to the NWS Honolulu rainfall-graph interface:

```text
https://www.weather.gov/hfo/rra_graphs
```

Do not merely provide an architecture description, pseudocode, isolated examples, or incomplete scaffolding. Generate a complete, runnable repository containing all source files, JTE templates, JavaScript, CSS, tests, fixtures, configuration, database migrations, build files, documentation, and Maven Wrapper files.

## 1. Application identity

Use these identifiers consistently:

```text
Application name: Pembana Rain Gauge
Primary brand: Pembana
Product name: Rain Gauge
Maven artifact ID: pembana-rain-gauge
Repository name: pembana-rain-gauge
Base Java package: com.pembana.raingauge
Default page title: Pembana Rain Gauge — Hawaiʻi Rainfall Station Data
```

In the visual design:

* emphasize **Pembana** as the primary brand;
* display **Rain Gauge** as the descriptive product name;
* use “Pembana Rain Gauge” in metadata, documentation, and the application title;
* do not display trademark symbols unless explicitly configured.

## 2. Primary objectives

Build an application that:

1. Retrieves the Hawaiʻi weather-station catalog dynamically.
2. Stores station metadata in a relational database.
3. Uses an embedded in-memory H2 database by default.
4. Supports PostgreSQL through a Spring profile.
5. Retrieves the catalog during startup when the local station table is empty.
6. Periodically refreshes the station catalog.
7. Applies a small configuration overlay for aliases, preferred names, island grouping, featured stations, and manually disabled stations.
8. Retrieves raw precipitation observations from remote NOAA/NWS-related endpoints.
9. Calculates rainfall totals from native observations instead of estimating values from graph images.
10. Clearly identifies complete, partial, stale, conflicting, and unavailable results.
11. Renders complete JTE pages that work without JavaScript.
12. Uses JavaScript and the Fetch API to update the dashboard when a user selects another station.
13. Uses Apache ECharts for rainfall graphs.
14. Uses Tabler for a modern responsive interface.
15. Follows the Spring Framework coding style.

The principal example station is:

```text
NWS station ID: WIHH1
Weather.gov alias: HI82
Preferred display name: Waiaha
Network: HI_DCP
Location description: WAIAHA NEAR KAILUA-KONA 3SE
Primary precipitation SHEF variable: PCIRG
Native precipitation unit: inches
Island: Hawaiʻi
Region: North Kona
```

Include `WIHH1` in the default configuration overlay as a featured station.

## 3. Required technology stack

Use:

* Java 21
* Spring Boot 4.1.0
* Spring Framework 7
* Maven
* Maven Wrapper
* Spring MVC
* Spring Data JPA
* Hibernate ORM
* H2
* PostgreSQL
* Flyway
* JTE 3.2.4
* JTE Spring Boot 4 starter
* JTE Maven plugin
* Tabler
* Tabler Icons
* Apache ECharts
* vanilla JavaScript ES modules
* browser Fetch API
* Spring `RestClient`
* Spring Boot Validation
* Spring Boot Actuator
* Caffeine
* Jackson
* JSpecify
* JUnit Jupiter
* AssertJ
* Mockito
* MockMvc
* Testcontainers PostgreSQL
* WireMock, MockWebServer, or an equivalent HTTP test server

Prefer dependencies managed by Spring Boot.

Pin frontend library versions. Do not load floating `latest` versions from CDNs.

Use locally vendored production assets under:

```text
src/main/resources/static/vendor/
```

Include appropriate third-party license and notice files.

Do not use:

* WebFlux;
* Lombok;
* field injection;
* Thymeleaf;
* JSP;
* jQuery;
* React;
* Angular;
* Vue;
* a single-page application framework;
* MongoDB;
* Redis;
* a repository adapter layer;
* duplicate domain and persistence representations of `Station`;
* floating-point arithmetic for rainfall;
* graph-pixel analysis;
* arbitrary user-supplied upstream URLs;
* incomplete `TODO` implementations;
* placeholder exceptions in the completed repository.

## 4. Verify remote sources before implementation

Before coding a parser, make representative HTTP requests to the remote services and inspect the actual responses.

Do not invent schemas based only on this prompt.

Save representative, sanitized responses under:

```text
src/test/resources/fixtures/
```

Tests must use these fixtures and must not depend on live internet access by default.

## 5. Dynamic station discovery

Retrieve the Hawaiʻi HADS/DCP station catalog from:

```text
GET https://mesonet.agron.iastate.edu/geojson/network.py?network=HI_DCP
```

Optionally retrieve only stations currently classified as online through:

```text
GET https://mesonet.agron.iastate.edu/geojson/network.py
    ?network=HI_DCP
    &only_online=1
```

Use the complete catalog as the primary catalog source. Preserve temporarily offline stations rather than deleting or hiding them automatically.

The response is GeoJSON. Parse it into an external response model and then create or update `Station` entities.

Preserve every useful field actually returned, including where available:

* network;
* station ID;
* station name;
* latitude;
* longitude;
* archive start;
* archive end;
* online status;
* elevation;
* state;
* country;
* time zone;
* source metadata.

Do not assume all possible properties are always present.

Optionally support network discovery through:

```text
GET https://mesonet.agron.iastate.edu/geojson/networks.py
```

## 6. Rainfall-capability discovery

A station’s presence in `HI_DCP` does not prove that it currently provides usable precipitation data.

Use this endpoint as one source for discovering recently available variables:

```text
GET https://mesonet.agron.iastate.edu/json/dcp_vars.py?station=WIHH1
```

Do not treat that response as a permanent capability registry. A station may be temporarily silent or may not have reported a particular variable during the endpoint’s current reporting window.

Use a combination of:

1. station-variable discovery;
2. HADS metadata;
3. variables present in recent raw observations;
4. explicit local configuration overrides.

Classify stations with:

```java
public enum RainfallCapability {

    SUPPORTED_ACCUMULATOR,

    SUPPORTED_INTERVAL_PRECIPITATION,

    PRECIPITATION_TYPE_UNKNOWN,

    NO_PRECIPITATION_VARIABLE,

    TEMPORARILY_SILENT,

    UNSUPPORTED
}
```

Do not classify a station from words such as “rain,” “stream,” “reservoir,” or “gauge” in its name.

## 7. Observation source

Use the IEM HADS archive endpoint as the primary scriptable source for raw SHEF observations:

```text
https://mesonet.agron.iastate.edu/cgi-bin/request/hads.py
```

Its documentation is available at:

```text
https://mesonet.agron.iastate.edu/cgi-bin/request/hads.py?help=
```

A representative request is:

```text
GET https://mesonet.agron.iastate.edu/cgi-bin/request/hads.py
    ?stations=WIHH1
    &network=HI_DCP
    &sts=2026-07-01T00:00Z
    &ets=2026-07-18T00:00Z
    &what=txt
    &delim=comma
```

Construct URIs with Spring’s URI-building facilities. Do not embed whitespace or line breaks in actual requests.

Requirements:

* send UTC query boundaries;
* support one or more station IDs when the endpoint allows batching;
* parse the actual returned header;
* validate columns rather than assuming fixed positions;
* preserve station ID;
* preserve valid timestamp;
* preserve received timestamp when supplied;
* preserve SHEF key;
* preserve numeric value;
* preserve qualifier;
* preserve source code;
* preserve unit metadata where available;
* preserve other quality or transmission fields;
* reject malformed rows individually where possible;
* record parser warnings;
* expose parser warnings through data-quality results.

## 8. NOAA HADS metadata

Use NOAA HADS metadata to verify station and SHEF-variable behavior.

Representative metadata endpoints include:

```text
https://hads.ncep.noaa.gov/cgi-bin/hads/interactiveDisplays/displayMetaData.pl
    ?nwsli=WIHH1
    &table=dcp
```

Known WIHH1 metadata may also be accessed using:

```text
https://hads.ncep.noaa.gov/cgi-bin/hads/interactiveDisplays/displayMetaData.pl
    ?nesdis_id=9321011E
    &table=dcp
```

SHEF variable descriptions may be accessed using:

```text
https://hads.ncep.noaa.gov/cgi-bin/hads/interactiveDisplays/displaySHEF.pl
    ?shef_code=PCIRG
```

Do not assume every station:

* reports every five minutes;
* reports at regular intervals;
* uses `PCIRG`;
* uses inches;
* uses the same reset behavior;
* has complete metadata;
* has uninterrupted historical data.

Determine the actual cadence from metadata and observation timestamps.

The requirement to use “raw five-minute data” means “use the most granular native observations available.” It does not permit falsely describing hourly or 15-minute observations as five-minute data.

## 9. Validation sources

Use validation sources to identify discrepancies. Do not silently replace missing native observations with summary data and then label the result exact.

IEM daily API:

```text
GET https://mesonet.agron.iastate.edu/api/1/daily.json
    ?network=HI_DCP
    &station=WIHH1
    &year=2026
    &month=7
```

NWS Honolulu rainfall report:

```text
https://forecast.weather.gov/product.php
    ?issuedby=HFO
    &product=RRA
    &site=hfo
```

NWS hourly hydrometeorological text product:

```text
https://tgftp.nws.noaa.gov/data/raw/sr/srhw70.phfo.rr5.hfo.txt
```

The NWS rainfall-graph site is a user-interface and station-selection reference:

```text
https://www.weather.gov/hfo/rra_graphs
```

Do not:

* scrape graph pixels;
* estimate values by visually reading graphs;
* present graph-derived estimates as exact;
* average conflicting source totals.

## 10. Persistence architecture

Keep persistence straightforward.

Use this dependency flow:

```text
MVC or API controller
        ↓
StationService
        ↓
StationRepository extends JpaRepository
        ↓
Hibernate
        ↓
H2 or PostgreSQL
```

Controllers must depend on `StationService`.

`StationService` must depend directly on `StationRepository`.

Do not introduce:

* a separate repository abstraction;
* a JPA repository adapter;
* a station persistence mapper;
* a duplicate station-domain type;
* persistence implementations for non-relational databases.

`StationRepository` must extend `JpaRepository`.

Example:

```java
package com.pembana.raingauge.station;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository
        extends JpaRepository<Station, UUID> {

    Optional<Station> findByNetworkAndStationId(
            String network, String stationId);

    boolean existsByNetworkAndStationId(
            String network, String stationId);

    List<Station> findAllByEnabledTrueOrderByDisplayNameAsc();

    List<Station> findAllByFeaturedTrueAndEnabledTrueOrderByDisplayNameAsc();
}
```

Add only repository methods actually used.

Prefer:

* derived Spring Data queries for simple queries;
* JPQL for more complex portable queries;
* pagination for large results.

Avoid native queries unless genuinely necessary.

## 11. Station entity

Use the JPA entity as the application’s station model.

Example shape:

```java
@Entity
@Table(
        name = "weather_station",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weather_station_network_station_id",
                columnNames = {"network", "station_id"}))
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String network;

    @Column(name = "station_id", nullable = false, length = 32)
    private String stationId;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(length = 32)
    private String alias;

    @Column(length = 64)
    private String island;

    @Column(length = 128)
    private String region;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "archive_begin")
    private LocalDate archiveBegin;

    @Column(name = "archive_end")
    private LocalDate archiveEnd;

    @Column(name = "source_online", nullable = false)
    private boolean sourceOnline;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean featured;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "rainfall_capability",
            nullable = false,
            length = 48)
    private RainfallCapability rainfallCapability;

    @Column(name = "precipitation_key", length = 16)
    private String precipitationKey;

    @Column(name = "latest_observation_at")
    private Instant latestObservationAt;

    @Column(name = "catalog_last_seen_at")
    private Instant catalogLastSeenAt;

    @Column(name = "catalog_refreshed_at")
    private Instant catalogRefreshedAt;

    protected Station() {
    }

    // Generate complete constructors, getters, and focused mutation methods.
}
```

Use `BigDecimal` for coordinates.

Do not use Lombok.

Use focused mutation methods such as:

```java
public void updateSourceMetadata(
        String sourceName,
        @Nullable BigDecimal latitude,
        @Nullable BigDecimal longitude,
        boolean sourceOnline,
        @Nullable LocalDate archiveBegin,
        @Nullable LocalDate archiveEnd,
        Instant refreshedAt) {
    // Complete implementation.
}

public void applyOverride(StationOverride override) {
    // Complete implementation.
}

public void markNotSeenDuringRefresh(Instant refreshedAt) {
    // Complete implementation.
}
```

Preserve both upstream and effective display values where applicable.

Do not overwrite `sourceName` with a configured preferred name.

## 12. Default H2 database

Use an embedded H2 database held entirely in memory by default.

Example:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:pembana-rain-gauge;DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Consequences:

* the default database is ephemeral;
* restarting the application removes station records;
* the application retrieves the station catalog again after restart when the table is empty.

Use Flyway for schema management.

Do not use Hibernate `create`, `create-drop`, or `update` as the application’s schema-management strategy.

## 13. PostgreSQL profile

Provide a `postgres` Spring profile.

Example:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Use the same repository and entities for H2 and PostgreSQL.

Do not create separate H2 and PostgreSQL repository implementations.

Write portable Flyway migrations.

Run PostgreSQL integration tests with Testcontainers.

## 14. Station configuration overlay

Retain a small type-safe configuration overlay.

The overlay is for:

* aliases;
* preferred display names;
* island assignment;
* region assignment;
* featured status;
* manual enablement or disablement;
* explanatory notes;
* documented precipitation-key overrides.

Example:

```yaml
hawaii:
  rainfall:
    station-overrides:
      WIHH1:
        alias: HI82
        preferred-name: Waiaha
        island: Hawaiʻi
        region: North Kona
        enabled: true
        featured: true
        precipitation-key: PCIRG
        note: Waiaha station near Kailua-Kona
```

Support:

```java
public record StationOverride(
        @Nullable String alias,
        @Nullable String preferredName,
        @Nullable String island,
        @Nullable String region,
        @Nullable Boolean enabled,
        @Nullable Boolean featured,
        @Nullable String disabledReason,
        @Nullable String precipitationKey,
        @Nullable String note) {
}
```

Use `@ConfigurationProperties` and Jakarta Bean Validation.

Override precedence:

```text
configured value when present
otherwise upstream value
```

A disabled station must:

* remain in the database;
* remain visible to administrators;
* be excluded from the public station selector;
* retain all upstream metadata;
* become selectable again when re-enabled.

## 15. Initial station loading

Implement a `StationCatalogBootstrapper` with `ApplicationRunner`.

At startup:

1. Call `StationService.initializeCatalogIfEmpty()`.
2. Check whether `StationRepository.count()` is zero.
3. If records exist, skip the initial remote catalog request.
4. If the table is empty, retrieve the remote `HI_DCP` catalog.
5. Validate and map remote records.
6. Apply configuration overlays.
7. save records in a bounded transaction;
8. record the refresh time;
9. log catalog statistics.

The bootstrap must be idempotent.

Do not keep a transaction open during a remote HTTP request.

Use this sequence:

```text
check repository
        ↓
fetch remote catalog outside a transaction
        ↓
validate and prepare records
        ↓
open a short transaction
        ↓
merge and save
```

Use `TransactionTemplate` inside `StationService` for the short merge transaction. Do not introduce an adapter or an extra persistence abstraction merely to establish a transaction boundary.

Example dependency:

```java
@Service
public class StationService {

    private final StationRepository stationRepository;

    private final IemStationCatalogClient stationCatalogClient;

    private final RainfallCapabilityService rainfallCapabilityService;

    private final StationOverrideProperties stationOverrideProperties;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    // Complete constructor and implementation.
}
```

When the local catalog is empty and the provider is unavailable:

* allow startup by default;
* log the failure;
* display an explicit empty-state warning;
* expose degraded provider health;
* permit scheduled and administrator-triggered retry.

Support:

```yaml
hawaii:
  rainfall:
    catalog:
      network: HI_DCP
      refresh-interval: 24h
      fail-startup-when-empty: false
```

If `fail-startup-when-empty` is true, fail startup only when:

* the table is empty; and
* the remote station catalog cannot be loaded.

## 16. Catalog refresh

Refresh the catalog on a configurable schedule, defaulting to every 24 hours.

A refresh must:

* retrieve the complete catalog;
* update existing stations;
* add new stations;
* retain manually configured enablement;
* retain aliases and preferred names;
* update online status;
* update capability metadata;
* retain stations absent from one response;
* mark absent stations as unconfirmed;
* not immediately delete missing stations;
* record refresh timestamps;
* continue serving existing records if refresh fails.

Do not refresh the entire station catalog during a normal page request.

Provide an administrator endpoint:

```text
POST /admin/station-catalog/refresh
```

Protect it with Spring Security, authorization, and CSRF protection.

## 17. Observation persistence and caching

Do not persist raw precipitation observations in the first implementation.

Use this dependency flow:

```text
Rainfall controller
        ↓
RainfallService
        ↓
ObservationService
        ↓
HadsObservationClient
```

`ObservationService` should:

* retrieve raw remote observations;
* parse observations;
* cache successful responses with Caffeine;
* reuse overlapping cached ranges where practical;
* expose fetch time and cache age;
* support stale-cache fallback;
* avoid caching malformed responses as successes.

Do not create an `ObservationStore` abstraction.

Persist station metadata through JPA. Keep raw observations remote and cached.

## 18. Numerical correctness

Use `BigDecimal` for all rainfall values and calculations.

Never use:

* `double`;
* `float`;
* binary floating-point rainfall totals.

Preserve the native source scale.

Use exact conversion:

```text
millimeters = inches × 25.4
```

Display:

* imperial rainfall to `0.01 in`;
* metric rainfall to `0.1 mm`.

Round only during presentation.

Do not round intermediate values repeatedly.

Make clear that:

* display precision may exceed sensor resolution;
* `0.01 in` equals `0.254 mm`;
* showing `0.1 mm` does not mean the sensor measures in `0.1 mm` increments.

## 19. Accumulator interpretation

`PCIRG` is an accumulator and must not be treated as interval rainfall.

Implement a dedicated `RainfallAccumulator`.

Use this domain flow:

```text
raw observations
        ↓
sort and deduplicate
        ↓
validate qualifiers
        ↓
identify baseline
        ↓
calculate accumulator deltas
        ↓
classify resets and gaps
        ↓
produce RainfallResult
```

For consecutive valid readings:

```text
delta = current accumulator − previous accumulator
```

When `delta` is nonnegative, add it to the rainfall total.

Repeated values mean no recorded accumulation between those observations.

A negative delta may represent:

* reset;
* rollover;
* instrument restart;
* corrected observation;
* conflicting retransmission;
* corrupted data.

Do not automatically interpret every negative delta as rainfall.

## 20. Calculation windows

Use:

```java
ZoneId.of("Pacific/Honolulu")
```

Do not hard-code a UTC offset.

Represent periods as half-open intervals:

```text
[from, to)
```

For each requested period:

1. calculate local boundaries in `Pacific/Honolulu`;
2. convert query boundaries to UTC;
3. request a valid baseline observation at or before `from`;
4. request observations through `to`;
5. sort by valid timestamp;
6. deduplicate retransmissions;
7. calculate deltas;
8. include observations according to half-open interval semantics;
9. report the actual last observation used.

Do not interpolate an ending-boundary value.

Support:

* previous 1 hour;
* previous 3 hours;
* previous 6 hours;
* previous 12 hours;
* previous 24 hours;
* previous 7 days;
* previous 28 days;
* month to date;
* calendar month;
* year to date;
* previous calendar year;
* custom local date/time range.

“Previous 28 days” means exactly:

```text
28 × 24 hours
```

Calendar totals must use Hawaiʻi-local calendar boundaries.

## 21. Reset and rollover handling

Classify negative changes carefully.

A reset may be accepted when corroborated by evidence such as:

* a new value at or near zero;
* known station-reset metadata;
* a known accumulator maximum;
* several internally consistent readings after the drop;
* valid source qualifiers;
* a predictable reset schedule.

When a reset is confidently recognized:

```text
rainfall before reset
    +
new accumulator value after reset
```

When it cannot be resolved:

* split the series into unambiguous segments;
* calculate only unambiguous rainfall;
* classify the result as partial;
* expose an unresolved-reset warning;
* include the affected timestamps.

Make reset thresholds configurable.

Do not silently discard unusually large positive increments. Flag them as suspected outliers.

## 22. Duplicate and corrected observations

For duplicate station, timestamp, and SHEF-key combinations:

* collapse exact duplicates;
* prefer valid non-missing observations;
* preserve qualifier metadata;
* use deterministic source priority;
* flag conflicting values;
* never average conflicting values.

For out-of-order observations:

* sort before calculating;
* retain original source order for diagnostics if useful.

## 23. Missing-data handling

Determine expected cadence from:

* station metadata;
* recent observed intervals;
* median or modal timestamp spacing;
* explicit local override when necessary.

Calculate:

* expected samples;
* received samples;
* completeness percentage;
* longest gap;
* first valid observation;
* last valid observation;
* unresolved reset count;
* conflicting observation count;
* parser-warning count;
* source age.

Never treat a missing interval as zero rainfall.

Do not interpolate missing rainfall.

Do not infer missing rainfall from nearby stations.

## 24. Exactness terminology

A result may be called `COMPLETE` only when:

* a valid baseline exists;
* the native observations cover the requested period sufficiently;
* no material gaps exist;
* no unresolved reset exists;
* no unresolved duplicate conflict exists;
* source resolution supports the displayed precision.

Use an enum such as:

```java
public enum RainfallResultStatus {

    COMPLETE,

    PARTIAL,

    UNAVAILABLE,

    STALE,

    CONFLICTING
}
```

The UI must not use the word “exact” merely because a number is formatted to two decimal places.

Every result should expose:

* value;
* unit;
* native unit;
* display scale;
* source resolution;
* requested start;
* requested end;
* covered start;
* covered end;
* observation cutoff;
* calculated time;
* status;
* warnings;
* completeness.

## 25. Validation against daily totals

For complete Hawaiʻi-local calendar days, compare the locally calculated raw-observation total with the IEM daily API when a daily total exists.

When totals differ:

* retain both values;
* log the discrepancy;
* show the discrepancy in the quality panel;
* do not average them;
* do not silently substitute one for the other.

The native observation calculation remains primary unless a documented source-specific rule states otherwise.

## 26. Suggested application structure

Use a straightforward feature-based structure:

```text
com.pembana.raingauge
├── PembanaRainGaugeApplication.java
├── config
├── station
│   ├── Station.java
│   ├── StationRepository.java
│   ├── StationService.java
│   ├── StationController.java
│   ├── StationApiController.java
│   ├── StationCatalogBootstrapper.java
│   ├── StationOverride.java
│   ├── StationOverrideProperties.java
│   ├── RainfallCapability.java
│   └── client
│       ├── IemStationCatalogClient.java
│       └── IemStationCatalogResponse.java
├── observation
│   ├── ObservationService.java
│   ├── PrecipitationObservation.java
│   ├── ObservationQuality.java
│   ├── HadsObservationParser.java
│   └── client
│       └── HadsObservationClient.java
├── rainfall
│   ├── RainfallService.java
│   ├── RainfallAccumulator.java
│   ├── RainfallAmount.java
│   ├── RainfallWindow.java
│   ├── RainfallResult.java
│   ├── RainfallResultStatus.java
│   └── RainfallDataQuality.java
├── dashboard
│   ├── DashboardController.java
│   ├── DashboardApiController.java
│   ├── DashboardResponse.java
│   └── DashboardView.java
└── support
    ├── ApiExceptionHandler.java
    ├── MvcExceptionHandler.java
    └── ProviderHealthIndicator.java
```

Keep:

* HTTP parsing out of controllers;
* rainfall calculation out of controllers;
* calculations out of JTE templates;
* database access out of templates;
* display formatting out of the accumulator;
* remote DTOs separate from API response DTOs.

## 27. Server-rendered pages

Use JTE to render complete pages.

Create:

```text
GET /
GET /stations
GET /stations/{stationId}
GET /compare
GET /about-data
```

### Dashboard `/`

Display:

* Pembana Rain Gauge branding;
* station selector;
* default featured station;
* selected unit;
* latest observation;
* data-source age;
* cards for 24 hours, 7 days, 28 days, month to date, and year to date;
* rainfall quality status;
* cumulative chart;
* daily rainfall chart;
* daily rainfall table;
* provisional-data notice;
* empty-state warning when the catalog is unavailable.

### Stations `/stations`

Display a searchable and filterable table with:

* station ID;
* alias;
* display name;
* source name;
* island;
* region;
* latitude;
* longitude;
* network;
* online status;
* enabled status;
* featured status;
* rainfall capability;
* precipitation variable;
* latest observation;
* archive beginning;
* archive ending;
* catalog refresh time.

Support filters for:

* island;
* name or station ID;
* online status;
* rainfall capability;
* enabled status;
* recent observations.

### Station detail `/stations/{stationId}`

Display:

* station metadata;
* source metadata;
* configured overrides;
* coordinates;
* alias;
* network;
* supported SHEF variable;
* actual observation cadence;
* source resolution;
* latest observation;
* common rainfall totals;
* custom range form;
* daily table;
* monthly table;
* optional native-observation table;
* quality events;
* source discrepancies;
* interactive graphs.

### Comparison `/compare`

Allow multiple enabled stations to be selected.

Display:

* common-period totals;
* completeness;
* latest observation;
* daily bars;
* cumulative lines;
* sortable comparison table.

Do not imply that different stations represent identical microclimates.

### About data `/about-data`

Explain:

* station-catalog source;
* observation source;
* `PCIRG`;
* accumulator calculations;
* resets;
* missing data;
* validation;
* units;
* display precision;
* provisional data;
* caching;
* application version;
* build timestamp.

## 28. JTE structure

Use reusable templates:

```text
src/main/jte
├── layout
│   └── page.jte
├── components
│   ├── navbar.jte
│   ├── stationSelector.jte
│   ├── rainfallCard.jte
│   ├── rainfallTable.jte
│   ├── qualityBadge.jte
│   ├── sourceStatus.jte
│   ├── alert.jte
│   └── pagination.jte
├── dashboard.jte
├── stations.jte
├── stationDetail.jte
├── compare.jte
└── aboutData.jte
```

Requirements:

* use typed template parameters;
* rely on JTE HTML escaping;
* do not construct HTML in Java strings;
* do not call services from templates;
* do not perform calculations in templates;
* use development mode only in the development profile;
* precompile JTE templates during Maven builds;
* ensure the Maven plugin and runtime use compatible versions.

## 29. Tabler design

Use Tabler for a modern dashboard.

Include:

* responsive navigation;
* clean typography;
* mobile station selector;
* summary cards;
* loading skeletons;
* badges;
* responsive tables;
* dark and light mode;
* empty states;
* quality warnings;
* accessible forms;
* keyboard focus states;
* error alerts;
* Tabler Icons.

Do not copy the visual styling of Weather.gov.

The design should feel like a modern environmental-data dashboard.

## 30. Dynamic station selection with AJAX

The initial page must be fully server-rendered.

Render a standard GET form:

```html
<form method="get" action="/" id="station-form">
    <label for="station-selector">Weather station</label>
    <select name="station" id="station-selector">
        <!-- Render enabled stations through JTE. -->
    </select>
    <button type="submit">View station</button>
</form>
```

Without JavaScript, submitting the form must reload the page with the selected station.

With JavaScript, progressively enhance the selector.

When a user selects a different station, dynamically update:

* page heading;
* document title;
* station metadata;
* rainfall cards;
* latest observation;
* source age;
* quality panel;
* period table;
* daily table;
* cumulative chart;
* daily chart;
* warnings;
* URL query parameters.

Do not implement a single-page application.

## 31. Dashboard API

Use one aggregate request for the data immediately needed after station selection:

```text
GET /api/stations/{stationId}/dashboard
    ?period=28d
    &unit=imperial
```

Use a response similar to:

```json
{
  "station": {
    "stationId": "WIHH1",
    "alias": "HI82",
    "displayName": "Waiaha",
    "sourceName": "KAILUA-KONA 3SE - WAIAHA",
    "island": "Hawaiʻi",
    "region": "North Kona",
    "latitude": 19.0,
    "longitude": -155.0
  },
  "selection": {
    "period": "28d",
    "unit": "imperial",
    "from": "2026-06-20T09:00:00-10:00",
    "to": "2026-07-18T09:00:00-10:00"
  },
  "calculatedAt": "2026-07-18T19:03:11Z",
  "observationCutoff": "2026-07-18T18:55:00Z",
  "summary": {
    "oneHour": {},
    "threeHours": {},
    "sixHours": {},
    "twelveHours": {},
    "twentyFourHours": {},
    "sevenDays": {},
    "twentyEightDays": {},
    "monthToDate": {},
    "yearToDate": {}
  },
  "quality": {},
  "dailyRainfall": [],
  "charts": {
    "increments": [],
    "daily": [],
    "cumulative": []
  },
  "source": {}
}
```

Use the same `RainfallService` for MVC and JSON responses.

Do not implement separate calculations for JTE and API routes.

Provide separate APIs for optional large datasets:

```text
GET /api/stations
GET /api/stations/{stationId}
GET /api/stations/{stationId}/observations
GET /api/stations/{stationId}/monthly
GET /api/stations/{stationId}/quality-events
GET /api/compare
```

Validate:

* station ID;
* enabled status;
* period;
* unit;
* date range;
* range maximum;
* bucket size.

Use RFC 9457 problem details for API errors.

## 32. JavaScript behavior

Create:

```text
src/main/resources/static/js/station-dashboard.js
```

Use an ES module.

When the selected station changes:

1. cancel any previous request with `AbortController`;
2. set the dashboard region to `aria-busy="true"`;
3. show Tabler loading skeletons;
4. fetch the dashboard JSON;
5. validate the response status;
6. verify required response fields;
7. update text through `textContent`;
8. update tables with safe DOM creation;
9. update ECharts with `setOption`;
10. update the document title;
11. update the URL only after success;
12. remove the loading state;
13. preserve the last valid dashboard when a new request fails;
14. display an inline error;
15. prevent stale responses from overwriting newer data.

Do not insert provider-controlled strings through unsafe `innerHTML`.

## 33. Browser history

Synchronize the URL with the station selection:

```text
/?station=WIHH1&period=28d&unit=imperial
```

After a successful AJAX update:

* use `history.pushState()` for user-initiated station changes;
* use `history.replaceState()` when normalizing defaults.

Implement `popstate`.

Back and Forward navigation must restore the selected station and dashboard.

A copied URL must reproduce the same selection during initial server rendering.

## 34. ECharts requirements

Use Apache ECharts for:

1. native-interval rainfall increments;
2. daily rainfall bars;
3. cumulative rainfall;
4. multi-station comparisons.

Create each chart instance once.

When the station changes:

* show the chart loading state;
* update existing instances;
* replace data with `setOption`;
* preserve zoom only when reasonable;
* resize after layout changes;
* dispose only when the DOM node is permanently removed.

Charts must include:

* Hawaiʻi-local timestamps;
* tooltips with inches and millimeters;
* source status;
* data zoom;
* responsive resizing;
* textual titles;
* missing-data gaps;
* quality flags where practical.

Do not connect a line across missing data in a way that implies continuous observations.

Provide server-rendered table equivalents for all important chart data.

## 35. HTTP-client behavior

Use Spring `RestClient`.

Configure:

* connection timeout;
* read timeout;
* descriptive User-Agent;
* gzip;
* safe URI building;
* bounded payload handling;
* controlled retries;
* exponential backoff for `429` and transient `5xx`;
* no retry for normal client errors;
* stale-cache fallback;
* configurable provider base URLs.

Do not dump complete provider responses into ordinary logs.

Log:

* provider;
* station;
* requested range;
* duration;
* HTTP status;
* parsed row count;
* rejected row count;
* warning count.

## 36. Caching

Use Caffeine.

Cache:

* station-variable metadata for several hours;
* HADS station metadata for a longer period;
* recent observation responses briefly;
* immutable historical ranges longer;
* dashboard aggregate responses briefly.

Cache keys must include:

* station ID;
* SHEF key;
* start;
* end;
* relevant source parameters.

Expose:

* fetch time;
* cache age;
* stale-cache use;
* provider source.

Do not cache errors as valid empty results.

## 37. Security

Use Spring Security.

Publicly allow:

```text
GET /
GET /stations
GET /stations/**
GET /compare
GET /about-data
GET /api/stations/**
GET /api/compare
GET /actuator/health
```

Protect:

```text
POST /admin/station-catalog/refresh
```

Requirements:

* CSRF protection;
* administrator authorization;
* no arbitrary provider URL input;
* station IDs selected from the persisted enabled catalog;
* safe error responses;
* no stack traces in production pages;
* security headers;
* reasonable API range limits.

## 38. Observability

Use Spring Boot Actuator and Micrometer.

Expose health or metrics for:

* station-catalog provider;
* observation provider;
* last successful catalog refresh;
* last successful observation fetch;
* provider latency;
* parser warnings;
* malformed rows;
* unresolved resets;
* source discrepancies;
* stale-cache fallbacks;
* cache hits and misses;
* age of newest station observations.

A temporary failure for one station must not make the whole application globally unhealthy.

Represent provider degradation separately.

## 39. Spring Framework coding style

Follow the current Spring Framework code-style guide:

```text
https://github.com/spring-projects/spring-framework/wiki/Code-Style
```

Apply these conventions throughout:

* UTF-8;
* LF line endings;
* tabs for indentation;
* no trailing whitespace;
* one top-level type per file;
* K&R braces;
* `else`, `catch`, and `finally` on the same line as the closing brace;
* approximately 90 characters per line;
* never exceed 120 characters;
* Javadoc around 80 characters;
* no wildcard imports;
* no production `var`;
* use `this.field` for instance fields;
* do not qualify ordinary method calls with `this`;
* use `@Override`;
* use descriptive names;
* organize private methods near callers;
* use JUnit Jupiter;
* use AssertJ;
* use Mockito;
* use the `Tests` suffix for test classes.

Use import groups in this order:

1. `java.*`
2. `javax.*`, if present
3. `jakarta.*`
4. third-party imports
5. `org.springframework.*`
6. static imports

Add:

```text
.editorconfig
checkstyle.xml
```

Run Checkstyle during Maven `verify`.

## 40. Null safety

Use JSpecify.

Add package-level `@NullMarked` declarations.

Use `@Nullable` where absence is valid.

Do not:

* return `null` collections;
* use `Optional` for entity fields;
* use `Optional` as controller parameters without reason;
* conceal missing metadata with empty strings.

Prefer:

* empty immutable collections;
* explicit nullable metadata;
* `Optional` for repository and lookup return values.

## 41. Testing

Tests must run offline by default.

### Required station tests

Test:

* empty H2 database triggers catalog retrieval;
* populated database skips bootstrap retrieval;
* repeated initialization is idempotent;
* failed bootstrap permits startup by default;
* strict bootstrap mode fails when appropriate;
* refresh adds new stations;
* refresh updates existing stations;
* absent stations are not immediately deleted;
* aliases are applied;
* preferred names are applied;
* island and region are applied;
* disabled stations are excluded from the public selector;
* source name remains distinct from display name;
* H2 Flyway migration succeeds;
* PostgreSQL Flyway migration succeeds;
* repository behavior works on H2 and PostgreSQL.

### Required accumulator tests

Test:

1. normal increase;
2. no-rain repeated values;
3. several increments;
4. reset to zero;
5. nonzero reset;
6. rollover;
7. unresolved negative delta;
8. duplicate identical observations;
9. conflicting duplicates;
10. out-of-order observations;
11. missing samples;
12. missing baseline;
13. boundary exactly at an observation;
14. boundary between observations;
15. Hawaiʻi-local midnight;
16. month boundary;
17. year boundary;
18. leap day;
19. inch-to-millimeter conversion;
20. display rounding;
21. several resets in a long range;
22. stale data;
23. malformed qualifiers;
24. source discrepancy.

Example:

```text
10.00
10.02
10.05
```

Expected:

```text
0.05 in
1.27 mm before presentation rounding
1.3 mm when displayed to 0.1 mm
```

Reset example:

```text
baseline: 10.00
reading:  10.05
reset:     0.00
reading:   0.03
```

When the reset is valid:

```text
0.08 in
```

### HTTP and parser tests

Test:

* representative station-catalog GeoJSON;
* missing GeoJSON fields;
* malformed station entry;
* representative HADS response;
* header reordering;
* malformed row;
* empty response;
* timeout;
* retry;
* `429`;
* transient `5xx`;
* permanent `4xx`;
* stale-cache fallback.

### MVC and API tests

Test:

* dashboard rendering;
* station selector rendering;
* disabled station rejection;
* invalid period;
* invalid unit;
* excessive custom range;
* consistent observation cutoff;
* RFC 9457 errors;
* empty catalog page;
* provisional-data warning;
* no-JavaScript station selection.

### JavaScript tests

Use a lightweight JavaScript test setup appropriate for ES modules.

Test:

* station selection;
* request cancellation;
* stale-response prevention;
* loading state;
* card updates;
* table updates;
* ECharts updates;
* URL updates;
* Back and Forward navigation;
* API error display;
* preservation of previous valid data.

Optional browser tests may use Playwright.

## 42. JTE rendering tests

Add tests that render important JTE templates using representative view models.

Verify:

* templates compile;
* dashboard renders;
* station table renders;
* quality warnings render;
* empty states render;
* special characters are escaped;
* missing optional metadata does not cause failures.

## 43. Required configuration

Provide:

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
src/main/resources/application-postgres.yml
```

Include type-safe settings for:

* network;
* provider URLs;
* timeouts;
* retries;
* cache durations;
* catalog refresh interval;
* startup failure policy;
* station overrides;
* maximum query range;
* default station;
* default period;
* default unit;
* administrator security.

## 44. Required repository contents

Generate:

```text
pom.xml
mvnw
mvnw.cmd
.mvn/wrapper/*
.editorconfig
.gitignore
checkstyle.xml
README.md
LICENSE
NOTICE
Dockerfile
compose.yaml
src/main/java/*
src/main/resources/application*.yml
src/main/resources/db/migration/*
src/main/resources/static/css/*
src/main/resources/static/js/*
src/main/resources/static/vendor/*
src/main/jte/*
src/test/java/*
src/test/resources/fixtures/*
```

Use an appropriate open-source license, such as Apache License 2.0, unless another license is specified.

## 45. README requirements

Document:

* project purpose;
* Pembana Rain Gauge branding;
* architecture;
* station-catalog source;
* observation source;
* validation sources;
* example requests;
* provisional-data warning;
* rainfall algorithm;
* accumulator semantics;
* reset handling;
* duplicate handling;
* missing-data policy;
* exactness terminology;
* precision and rounding;
* Hawaiʻi timezone handling;
* station overrides;
* H2 behavior;
* PostgreSQL setup;
* Flyway;
* caching;
* startup bootstrap;
* scheduled refresh;
* AJAX behavior;
* server-rendered fallback;
* local development;
* production build;
* tests;
* Docker;
* optional live-provider tests;
* known limitations.

## 46. Acceptance criteria

The repository is complete only when:

* `./mvnw verify` succeeds;
* Checkstyle succeeds;
* H2 Flyway migration succeeds;
* PostgreSQL Testcontainers migration succeeds;
* the application starts with H2;
* the catalog loads when the database is empty;
* the catalog is not fetched during startup when the database is populated;
* aliases and preferred names are applied;
* `WIHH1` displays as `Waiaha (HI82)`;
* `/` renders a Tabler dashboard;
* `/stations` renders a station table;
* `/stations/WIHH1` renders station detail;
* AJAX station selection works;
* normal GET station selection works without JavaScript;
* browser history works;
* ECharts updates without recreation;
* JTE templates precompile;
* native HADS observations are parsed;
* accumulator values are converted into rainfall deltas;
* calculations use `BigDecimal`;
* missing data are not converted to zero;
* unresolved resets produce partial results;
* source discrepancies are visible;
* provisional-data warnings are visible;
* raw observations are cached but not persisted;
* station metadata is persisted through Spring Data JPA;
* no separate repository adapter exists;
* no unfinished placeholder code remains.

## 47. Final generation instructions

When producing the repository:

1. Begin with a concise implementation summary.
2. Show the complete repository tree.
3. Generate every required file.
4. Do not omit files described as obvious or boilerplate.
5. Do not replace code with comments such as “implementation omitted.”
6. Do not leave `TODO` markers.
7. Do not leave placeholder exceptions.
8. Use complete Maven coordinates and compatible dependency versions.
9. Generate valid Flyway migrations.
10. Include representative upstream fixtures.
11. Run `./mvnw verify` when execution tools are available.
12. Fix build and test failures before reporting completion.
13. Report any remaining limitation honestly.
14. State the actual reporting cadence discovered for WIHH1.
15. Explain exactly how the WIHH1 rainfall total is derived.
16. State whether each displayed total is complete, partial, stale, conflicting, or unavailable.
17. Never claim that a graph estimate is an exact total.
