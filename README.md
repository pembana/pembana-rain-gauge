# Pembana Rain Gauge

Pembana Rain Gauge is a Java 21/Spring Boot 4.1 application for exploring near-real-time and
historical rainfall at Hawaiʻi HADS/DCP stations. **Pembana** is the primary brand; **Rain Gauge**
describes the product. It calculates totals from native station observations and makes gaps,
resets, conflicts, stale data, and unavailable data visible instead of presenting graph estimates
as exact measurements.

The initial response is a complete server-rendered JTE page. Vanilla JavaScript progressively
enhances station changes with Fetch, browser history, and locally vendored ECharts. The normal GET
forms continue to work with JavaScript disabled.

## Data sources and provenance

- Station discovery: IEM `HI_DCP` [GeoJSON catalog](https://mesonet.agron.iastate.edu/geojson/network.py?network=HI_DCP).
- Native observations: IEM [HADS archive request service](https://mesonet.agron.iastate.edu/cgi-bin/request/hads.py?help=).
- Recent variable discovery: IEM `dcp_vars.py`.
- Station and SHEF verification: NOAA HADS metadata and SHEF descriptions.
- Daily validation: IEM daily API. A differing daily value is retained beside the native result;
  values are never averaged or silently substituted.
- NWS Honolulu rainfall products are additional operational references, not sources used to fill
  missing native samples.

All upstream values are provisional and may be corrected later. Representative sanitized source
responses are committed in `src/test/resources/fixtures`; the default tests do not require the
internet.

### WIHH1 / HI82 / Waiaha

The featured default station is IEM/NWS station `WIHH1`, displayed as **Waiaha (HI82)** by the
configuration overlay. Live source inspection found `PCIRGZZ` and `PPHRGZZ`; the configured primary
key is normalized to `PCIRG`. NOAA metadata reports a 60-minute transmission interval and a
15-minute self-timed `PCIRG` sensor interval. The actual archive sample inspected was therefore
15-minute data—not five-minute data.

`PCIRG` is an accumulator in inches. For a requested half-open period `[from, to)`, the application:

1. requests an additional baseline lookback and selects the latest valid reading at or before
   `from`;
2. sorts readings by valid time and deterministically collapses retransmissions;
3. subtracts each previous accumulator from the next one;
4. adds nonnegative deltas whose timestamp falls inside the requested period;
5. accepts only corroborated near-zero resets or configured rollovers;
6. retains but flags unusually large positive increments; and
7. reports the last observation actually used as the cutoff.

For example, `10.00 → 10.02 → 10.05` produces `0.05 in`, exactly `1.270 mm`, displayed as `1.3 mm`.
A corroborated sequence `10.00 → 10.05 → 0.00 → 0.03` produces `0.08 in`. An unresolved negative
change is not counted as rainfall and makes the result partial.

## Status and numerical policy

Every displayed total is classified as `COMPLETE`, `PARTIAL`, `STALE`, `CONFLICTING`, or
`UNAVAILABLE`. A complete result requires a usable baseline, adequate native coverage, no material
gap, no unresolved reset, and no conflicting retransmission. Missing intervals are never converted
to zero, interpolated, borrowed from another station, or filled with a daily summary.

Rainfall uses `BigDecimal` end to end. Conversion is exactly `millimeters = inches × 25.4`.
Intermediate results are not repeatedly rounded. Presentation uses `0.01 in` or `0.1 mm`; this
formatting can be finer than the sensor resolution and does not make a total “exact.” Hawaiʻi-local
calendar windows use `Pacific/Honolulu`, while provider boundaries are sent in UTC. Previous 28
days means exactly 672 hours.

## Architecture

The repository is organized by feature and intentionally keeps the persistence path small:

```text
MVC/API controller ──> StationService ──> StationRepository (JpaRepository)
                                               │
                                               └──> Hibernate ──> H2/PostgreSQL

DashboardService ──> RainfallService ──> ObservationService ──> HadsObservationClient
                           │                     │
                           │                     └──> Caffeine (raw responses; no persistence)
                           └──> RainfallAccumulator
```

Remote DTOs, public response records, and the single JPA `Station` entity are separate concerns;
there is no repository adapter or duplicate station domain representation. Provider parsing and
rainfall calculations stay outside controllers and templates.

## Running locally

Requirements: Java 21. Docker is optional unless running PostgreSQL integration tests or the
PostgreSQL profile.

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The default profile uses the entirely in-memory
H2 URL `jdbc:h2:mem:pembana-rain-gauge`; all station records disappear on restart. If the station
table is empty, an `ApplicationRunner` fetches `HI_DCP` outside a transaction, then performs a short
transactional merge. A populated table skips the bootstrap. Provider failure permits startup by
default and produces a degraded health/empty-page state; set
`hawaii.rainfall.catalog.fail-startup-when-empty=true` for strict startup.

Useful requests:

```bash
curl 'http://localhost:8080/?station=WIHH1&period=28d&unit=imperial'
curl 'http://localhost:8080/api/stations/WIHH1/dashboard?period=28d&unit=metric'
curl 'http://localhost:8080/api/stations'
curl 'http://localhost:8080/actuator/health'
```

The catalog refreshes every 24 hours after the initial 24-hour delay. An administrator can trigger
the same merge explicitly; this endpoint requires HTTP Basic authentication, the `ADMIN` role, and
a CSRF token:

```text
POST /admin/station-catalog/refresh
```

The default `admin/change-me` account is only a local-development convenience. The production
profile requires `ADMIN_USERNAME` and an encoded `ADMIN_PASSWORD`.

JTE development mode is available with:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Station overlays

Upstream names and effective display values are retained separately. Small, reviewed overlays live
under `hawaii.rainfall.station-overrides` in `application.yml` and can set aliases, preferred names,
island/region grouping, featured state, manual enablement, a disabled reason, a precipitation key,
and an explanatory note. Disabled stations remain in the database and administrator catalog but
are absent from public selectors and APIs.

## PostgreSQL and Flyway

The same entity, repository, and portable Flyway migration are used for H2 and PostgreSQL.

```bash
docker compose up -d postgres
export DATABASE_URL='jdbc:postgresql://localhost:5432/pembana_rain_gauge'
export DATABASE_USERNAME='pembana'
export DATABASE_PASSWORD='pembana'
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Hibernate uses `ddl-auto: validate`; Flyway alone owns schema changes. The Testcontainers test runs
the migration and repository behavior against PostgreSQL 17 when Docker is available and skips
cleanly otherwise.

## Caching and provider behavior

Recent observation responses are cached for five minutes, historical ranges for 24 hours, and a
separate successful-response fallback for seven days. Station-variable metadata and daily summaries
use Spring/Caffeine caches. Cache keys contain station, network/SHEF key, and exact UTC boundaries;
covering ranges may satisfy smaller requests. A live failure can use the fallback only when a prior
successful response covers the request, and the result is explicitly `STALE`. Errors and malformed
responses are never cached as successful empty data.

Spring `RestClient` uses bounded payloads, configured connect/read timeouts, a descriptive
User-Agent, gzip, safe URI construction, and exponential retry for `429`/transient `5xx`. Ordinary
`4xx` responses are not retried. Logs record provider, station/range, duration, accepted/rejected
counts, and warnings without dumping full payloads.

## UI and API

Server pages:

- `/` — dashboard and standard GET station selector
- `/stations` and `/stations/{stationId}` — filterable catalog and station/custom-range detail
- `/compare` — multi-station totals, daily bars, cumulative lines, and table
- `/about-data` — methodology, provenance, precision, and build information

JSON APIs include `/api/stations`, `/api/stations/{stationId}`,
`/api/stations/{stationId}/dashboard`, `/observations`, `/monthly`, `/quality-events`, and
`/api/compare`. Invalid station IDs, disabled stations, periods, units, and excessive ranges use
RFC 9457 problem details.

The aggregate dashboard response drives both JTE and AJAX presentation. The ES-module controller
cancels superseded requests, prevents stale responses from winning, uses safe `textContent`/DOM
creation, keeps the last valid view on failure, and updates the URL only after success. ECharts
instances are created once and updated with `setOption`; important rainfall values also have
server-rendered tables.

Tabler Core 1.4.0, Tabler Icons 3.44.0, and Apache ECharts 6.1.0 are pinned and served from
`src/main/resources/static/vendor` with their license/notice files. No runtime CDN is required.

## Build and tests

```bash
./mvnw verify
```

The lifecycle precompiles all JTE templates, runs Java unit/MVC/parser/HTTP/cache tests, runs the
ES-module tests with Node, exercises H2/Flyway, conditionally exercises PostgreSQL/Testcontainers,
and runs Checkstyle. Tests use fixtures or local HTTP servers. There are no live-provider tests in
the default suite; a manual live smoke check can use the URLs above after starting the application.

Build the production artifact or container with:

```bash
./mvnw package
docker build -t pembana-rain-gauge .
```

## Known limitations

- Accumulator rainfall (`PC…`, including WIHH1 `PCIRG`) is calculated. Interval-precipitation
  variables (`PP…`) are discovered and classified but are not yet used for public totals.
- Runtime capability discovery combines explicit overrides and recent IEM variables; NOAA HADS
  metadata was used to verify the featured station and is retained as a fixture, but there is not
  yet a general NOAA metadata scraper for every station.
- Daily validation uses the IEM daily API. NWS RRA/RR products remain human verification sources.
- Raw observations are intentionally cache-only, so historical availability and corrections remain
  bounded by the upstream archive.
- H2 is ephemeral by design; use the PostgreSQL profile for durable station metadata.

## License

Pembana Rain Gauge is licensed under the [Apache License 2.0](LICENSE). Vendored dependencies retain
their own licenses under `src/main/resources/static/vendor`.
