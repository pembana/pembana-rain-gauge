# Upstream response fixtures

These sanitized, reduced fixtures were captured from the named public services
on 2026-07-19 before parser implementation. They retain actual field names and
representative values while reducing large responses to the records required
by offline tests.

* `iem-hi-dcp-catalog.json`: `GET /geojson/network.py?network=HI_DCP`; the live
  response contained 319 features.
* `iem-wihh1-vars.json`: `GET /json/dcp_vars.py?station=WIHH1`.
* `iem-wihh1-hads.csv`: `GET /cgi-bin/request/hads.py` for WIHH1; the actual
  response used a wide CSV header and 15-minute `PCIRGZZ` rows.
* `noaa-wihh1-metadata.html`: NOAA HADS station and SHEF metadata reduced to
  the relevant tables. It states a 15-minute self-timed PCIRG decode interval.
* `iem-wihh1-daily.json`: IEM daily API response reduced to two dates.

Tests never contact these providers unless an optional live-provider test is
explicitly enabled.
