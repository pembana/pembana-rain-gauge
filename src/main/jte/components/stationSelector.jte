@import java.util.List
@import com.pembana.raingauge.dashboard.DashboardView.StationMap
@import com.pembana.raingauge.station.StationResponse
@import com.pembana.raingauge.station.StationType
@import com.pembana.raingauge.rainfall.RainfallWindow
@import org.jspecify.annotations.Nullable

@param List<StationResponse> stations
@param String selectedStationId
@param String period
@param String unit
@param StationMap stationMap
@param boolean dailyStation
@param @Nullable String compare

<form method="get" action="/" id="station-form" class="station-form card card-sm">
  @if (compare != null)
    <input type="hidden" name="compare" value="${compare}">
  @endif
  <div class="card-body">
    <div class="row g-2 align-items-end">
      <div class="col-12 col-md">
        <label class="form-label" for="station-selector">Weather station</label>
        <div class="input-group">
          <select class="form-select" name="station" id="station-selector">
            @for (StationType type : StationType.values())
              @if (stations.stream().anyMatch(station -> station.type() == type))
                <optgroup label="${type == StationType.AUTOMATED ? "Automated gauges · HADS" : "Volunteer gauges · CoCoRaHS"}">
                  @for (StationResponse station : stations)
                    @if (station.type() == type)
                      <option value="${station.stationId()}" data-type="${type.name()}" selected="${station.stationId().equals(selectedStationId)}">${station.selectionLabel()}${station.archiveEnd() == null ? "" : " · archive through " + station.archiveEnd().getYear()}</option>
                    @endif
                  @endfor
                </optgroup>
              @endif
            @endfor
          </select>
          <button class="btn btn-outline-secondary station-map-trigger" type="button"
                  data-bs-toggle="modal" data-bs-target="#station-map-modal"
                  aria-label="Choose a weather station from the map" title="Choose from map">
            <svg xmlns="http://www.w3.org/2000/svg" class="icon-tabler m-0" width="24" height="24"
                 viewBox="0 0 24 24" stroke="currentColor" fill="none" aria-hidden="true">
              <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
              <path d="M12 21a9 9 0 1 0 0 -18a9 9 0 0 0 0 18"/>
              <path d="M3.6 9h16.8"/><path d="M3.6 15h16.8"/>
              <path d="M11.5 3a17 17 0 0 0 0 18"/>
              <path d="M12.5 3a17 17 0 0 1 0 18"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <label class="form-label" for="period-selector">${dailyStation ? "Reporting period" : "Chart period"}</label>
        <select class="form-select" name="period" id="period-selector">
          @if (!List.of("24h", "7d", "28d", "mtd", "ytd").contains(period))
            <option value="${period}" selected>${RainfallWindow.fromToken(period).label()}</option>
          @endif
          <option value="24h" selected="${period.equals("24h")}">${dailyStation ? "Today’s report" : "24 hours"}</option>
          <option value="7d" selected="${period.equals("7d")}">${dailyStation ? "7 report dates" : "7 days"}</option>
          <option value="28d" selected="${period.equals("28d")}">${dailyStation ? "28 report dates" : "28 days"}</option>
          <option value="mtd" selected="${period.equals("mtd")}">Month to date</option>
          <option value="ytd" selected="${period.equals("ytd")}">Year to date</option>
        </select>
      </div>
      <div class="col-6 col-md-2">
        <label class="form-label" for="unit-selector">Units</label>
        <select class="form-select" name="unit" id="unit-selector">
          <option value="imperial" selected="${unit.equals("imperial")}">Inches</option>
          <option value="metric" selected="${unit.equals("metric")}">Millimeters</option>
        </select>
      </div>
      <div class="col-12 col-md-auto">
        <button class="btn btn-primary w-100" type="submit" data-loading-message="Loading rainfall observations…">View station</button>
      </div>
    </div>
  </div>
</form>

<div class="modal fade" id="station-map-modal" tabindex="-1"
     aria-labelledby="station-map-title" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable modal-fullscreen-md-down">
    <div class="modal-content station-map-modal-content">
      <div class="modal-header">
        <div>
          <h2 class="modal-title" id="station-map-title">Rainfall stations across Hawaiʻi</h2>
          <fieldset class="map-legend mt-2">
            <legend class="visually-hidden">Show stations on the map</legend>
            <label><input class="visually-hidden" type="radio" name="map-network" value="all" checked><span>All stations</span></label>
            <label><input class="visually-hidden" type="radio" name="map-network" value="AUTOMATED"><span><i class="network-dot" aria-hidden="true"></i>Automated</span></label>
            <label><input class="visually-hidden" type="radio" name="map-network" value="DAILY_REPORTS"><span><i class="network-dot volunteer" aria-hidden="true"></i>CoCoRaHS</span></label>
          </fieldset>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <div class="station-map" id="station-map" role="region"
             aria-label="Interactive map of Hawaiʻi rainfall stations"
             data-tile-url="${stationMap.tileUrl()}"
             data-attribution-label="${stationMap.attributionLabel()}"
             data-attribution-url="${stationMap.attributionUrl()}">
          <div class="station-map-loading" data-map-loading>Loading map…</div>
        </div>
        <div hidden>
          @for (StationResponse station : stations)
            @if (station.latitude() != null && station.longitude() != null)
              <span data-station-map-pin data-station-id="${station.stationId()}"
                    data-type="${station.type().name()}"
                    data-station-label="${station.selectionLabel()}"
                    data-station-location="${station.island() == null ? "" : station.island()}${station.region() == null ? "" : " · " + station.region()}"
                    data-latitude="${station.latitude()}" data-longitude="${station.longitude()}"></span>
            @endif
          @endfor
        </div>
      </div>
      <div class="modal-footer justify-content-between">
        <span class="text-secondary">
          Select a pin to see the station and its network.
          <a href="https://www.openstreetmap.org/fixthemap" target="_blank" rel="noopener noreferrer">Report a map issue</a>.
        </span>
        <button type="button" class="btn" data-bs-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>
