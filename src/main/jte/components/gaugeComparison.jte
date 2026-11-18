@import com.pembana.raingauge.dashboard.DashboardView
@import com.pembana.raingauge.dashboard.GaugeComparisonService.Comparison
@import com.pembana.raingauge.dashboard.GaugeComparisonService.Nearby
@import com.pembana.raingauge.dashboard.GaugeComparisonService.Readings
@import com.pembana.raingauge.rainfall.RainfallResultStatus

@param DashboardView view
@param Comparison comparison

<section id="gauge-comparison" class="card mt-3 gauge-comparison" aria-labelledby="comparison-heading">
  <div class="card-header justify-content-between gap-2">
    <h3 class="card-title" id="comparison-heading">Nearby gauge comparison</h3>
    <a href="/?station=${view.selectedStation().stationId()}&amp;period=${view.period()}&amp;unit=${view.unit()}" class="small">Close comparison</a>
  </div>
  <div class="card-body">
    @if (!comparison.choices().isEmpty())
      <form method="get" action="/#gauge-comparison" class="row g-2 align-items-end mb-3">
        <input type="hidden" name="station" value="${view.selectedStation().stationId()}">
        <input type="hidden" name="period" value="${view.period()}">
        <input type="hidden" name="unit" value="${view.unit()}">
        <div class="col-sm">
          <label class="form-label" for="compare-station">${view.dailyStation() ? "Automated gauge" : "CoCoRaHS gauge"} · closest first</label>
          <select class="form-select" name="compare" id="compare-station">
            @for (Nearby choice : comparison.choices())
              <option value="${choice.station().getStationId()}" selected="${choice.equals(comparison.selected())}">${choice.label(view.unit())}</option>
            @endfor
          </select>
        </div>
        <div class="col-sm-auto"><button class="btn btn-outline-primary w-100" type="submit" data-loading-message="Comparing readings…">Compare gauges</button></div>
      </form>
    @endif
    !{Readings pair = comparison.readings();}
    @if (pair != null)
      <div class="comparison-window">
        <div class="subheader">Latest usable daily report · reading window</div>
        <strong>
          @if (pair.start() != null)
            ${DashboardView.readingTime(pair.start())} to
          @else
            Ending
          @endif
          ${DashboardView.readingTime(pair.end())}
        </strong>
      </div>
      <div class="comparison-readings">
        <div>
          <div class="network-label"><i class="network-dot volunteer" aria-hidden="true"></i>CoCoRaHS volunteer</div>
          <div class="h1 rainfall-value my-1">${pair.reportDisplay()}</div>
          @if (view.dailyStation())
            <div>${pair.volunteer().selectionLabel()}</div>
          @else
            <a href="/?station=${pair.volunteer().stationId()}&amp;period=${view.period()}&amp;unit=${view.unit()}">${pair.volunteer().selectionLabel()}</a>
          @endif
          @if (pair.staleReports())
            <span class="text-secondary">Previously cached report</span>
          @endif
        </div>
        <div>
          <div class="network-label"><i class="network-dot" aria-hidden="true"></i>Automated gauge</div>
          <div class="d-flex align-items-center flex-wrap gap-2 my-1">
            <strong class="h1 rainfall-value mb-0">${pair.gaugeDisplay()}</strong>
            @if (pair.gaugeStatus() != RainfallResultStatus.UNAVAILABLE)
              @template.components.qualityBadge(status = pair.gaugeStatus())
            @endif
          </div>
          @if (view.dailyStation())
            <a href="/?station=${pair.automated().stationId()}&amp;period=${view.period()}&amp;unit=${view.unit()}">${pair.automated().selectionLabel()}</a>
          @else
            <div>${pair.automated().selectionLabel()}</div>
          @endif
          @if (pair.gauge() != null && pair.gaugeStatus() != RainfallResultStatus.COMPLETE && pair.gaugeStatus() != RainfallResultStatus.UNAVAILABLE)
            <div class="small text-secondary mt-1">Samples cover ${DashboardView.observationTime(pair.gauge().coveredStart())} to ${DashboardView.observationTime(pair.gauge().coveredEnd())}.</div>
          @endif
        </div>
      </div>
      <p class="text-secondary small mb-0 mt-3">Each gauge measures rainfall at its own location. The automated total uses measurements inside the volunteer’s reading window. Volunteer data from <a href="https://www.cocorahs.org/Content.aspx?page=datausagepolicy">CoCoRaHS (CC BY 4.0)</a>.</p>
    @else
      <div class="text-secondary mb-1">Observation dates · ${view.reportDates().display()}</div>
    @endif
    @if (comparison.note() != null)
      <p class="mb-0 mt-2">${comparison.note()}</p>
    @endif
  </div>
</section>
