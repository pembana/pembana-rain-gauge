@import java.util.List
@import com.pembana.raingauge.station.StationResponse

@param List<StationResponse> stations
@param String selectedStationId
@param String period
@param String unit

<form method="get" action="/" id="station-form" class="station-form card card-sm">
  <div class="card-body">
    <div class="row g-2 align-items-end">
      <div class="col-12 col-md">
        <label class="form-label" for="station-selector">Weather station</label>
        <select class="form-select" name="station" id="station-selector">
          @for (StationResponse station : stations)
            <option value="${station.stationId()}" selected="${station.stationId().equals(selectedStationId)}">${station.selectionLabel()}</option>
          @endfor
        </select>
      </div>
      <div class="col-6 col-md-3">
        <label class="form-label" for="period-selector">Chart period</label>
        <select class="form-select" name="period" id="period-selector">
          <option value="24h" selected="${period.equals("24h")}">24 hours</option>
          <option value="7d" selected="${period.equals("7d")}">7 days</option>
          <option value="28d" selected="${period.equals("28d")}">28 days</option>
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
        <button class="btn btn-primary w-100" type="submit">View station</button>
      </div>
    </div>
  </div>
</form>
