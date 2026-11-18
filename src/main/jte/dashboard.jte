@import com.pembana.raingauge.dashboard.DashboardView
@import com.pembana.raingauge.dashboard.DashboardResponse
@import com.pembana.raingauge.station.StationResponse
@import com.pembana.raingauge.support.PageMetadata

@param DashboardView view
@param PageMetadata pageMetadata

@template.layout.page(
  title = view.title(),
  active = "dashboard",
  scriptModule = "/js/station-dashboard.js",
  pageMetadata = pageMetadata,
  content = @`
    <div class="page-header d-print-none">
      <div class="container-xl py-4">
        <div class="row align-items-center g-3">
          <div class="col">
            <div class="page-pretitle">Hawaiʻi environmental data</div>
            <h1 class="page-title">Rainfall station dashboard</h1>
            <p class="text-secondary mb-0">Explore automated gauges and daily CoCoRaHS reports across Hawaiʻi.</p>
          </div>
          <div class="col-auto">
            <span class="provisional-pill">Provisional data</span>
          </div>
        </div>
      </div>
    </div>
    <div class="page-body">
      <div class="container-xl">
        @if (view.catalogEmpty())
          @template.components.alert(tone = "warning", title = "Station catalog unavailable",
            message = "No local stations are available yet. The provider may be temporarily unavailable; automatic retry remains enabled.")
        @else
          @template.components.stationSelector(stations = view.stations(),
            selectedStationId = view.selectedStation().stationId(), period = view.period(), unit = view.unit(),
            stationMap = view.stationMap(), dailyStation = view.dailyStation(),
            compare = view.comparison() == null || view.comparison().selected() == null ? null : view.comparison().selected().station().getStationId())
        @endif
        <div id="dashboard-error" class="alert alert-danger mt-3 ${view.error() == null ? "d-none" : ""}" role="alert">
          <strong>Could not update rainfall data.</strong>
          <span data-error-message>${view.error() == null ? "" : view.error()}</span>
        </div>
        @if (view.selectedStation() != null)
          <section id="dashboard-region" class="dashboard-region mt-3" aria-live="polite" aria-busy="false"
                   data-station-id="${view.selectedStation().stationId()}" data-type="${view.selectedStation().type().name()}"
                   data-period="${view.period()}" data-unit="${view.unit()}">
            <div class="station-heading card station-hero ${view.dailyStation() ? "volunteer-hero" : ""}">
              <div class="card-body">
                <div class="row align-items-center g-3">
                  <div class="col">
                    <div class="network-label mb-1"><i class="network-dot ${view.dailyStation() ? "volunteer" : ""}" aria-hidden="true"></i>
                      ${view.dailyStation() ? "CoCoRaHS · Volunteer gauge" : "HADS · Automated gauge"}
                    </div>
                    <h2 class="mb-1" data-station-heading>${view.selectedStation().selectionLabel()}</h2>
                    <div class="text-secondary">
                      <span data-station-location>${view.selectedStation().island() == null ? "Hawaiʻi" : view.selectedStation().island()}${view.selectedStation().region() == null ? "" : " · " + view.selectedStation().region()}</span>
                      <span data-station-code hidden="${view.selectedStation().alias() == null}"> · <code data-station-id>${view.selectedStation().stationId()}</code></span>
                      @if (view.dailyStation())
                        · <a href="/stations/${view.selectedStation().stationId()}?unit=${view.unit()}&amp;to=${view.reportDates().to().toString()}&amp;from=${view.reportDates().from().toString()}">Report details</a>
                      @else
                        · <a data-station-detail href="/stations/${view.selectedStation().stationId()}?unit=${view.unit()}">Station details</a>
                      @endif
                    </div>
                  </div>
                  <div class="col-12 col-sm-auto text-sm-end">
                    <div class="subheader">${view.dailyStation() ? "Latest reading in period" : "Latest observation"}</div>
                    @if (view.dailyStation())
                      <div class="fw-semibold">${view.reports() == null ? "Unavailable" : DashboardView.readingTime(view.reports().latestReadingInRange())}</div>
                    @else
                      <div class="fw-semibold" data-observation-cutoff>${view.dashboard() == null ? "Unavailable" : DashboardView.observationTime(view.dashboard().observationCutoff())}</div>
                    @endif
                  </div>
                </div>
                @if (view.comparison() == null)
                  <a class="btn btn-sm btn-outline-primary mt-3" data-nearby-link
                     href="/?station=${view.selectedStation().stationId()}&amp;period=${view.period()}&amp;unit=${view.unit()}&amp;compare=nearest#gauge-comparison"
                     data-loading-message="Comparing nearby gauges…">${view.dailyStation() ? "Compare with a nearby automated gauge" : "Compare with a nearby CoCoRaHS gauge"}</a>
                @endif
              </div>
            </div>
            @if (view.comparison() != null)
              @template.components.gaugeComparison(view = view, comparison = view.comparison())
            @endif
            @if (view.dailyStation())
              @if (view.selectedStation().archiveEnd() != null && view.selectedStation().archiveEnd().isBefore(view.reportDates().from()))
                <section class="card card-body mt-3">
                  <h3 class="card-title">Archived CoCoRaHS station</h3>
                  <p>This station’s archive ends ${view.selectedStation().archiveEnd().toString()}, before your selected dates.</p>
                  <div><a href="/stations/${view.selectedStation().stationId()}?to=${view.selectedStation().archiveEnd().toString()}&amp;unit=${view.unit()}">View its last 28 report dates</a></div>
                </section>
              @elseif (view.reports() != null)
                @template.components.reportDashboard(reports = view.reports(), dates = view.reportDates())
              @endif
            @elseif (view.dashboard() != null)
              <div class="row row-cards mt-1 summary-grid station-summary-grid">
                @template.components.rainfallCard(key = "twentyFourHours", label = "24 hours", result = view.dashboard().summary().twentyFourHours())
                @template.components.rainfallCard(key = "sevenDays", label = "7 days", result = view.dashboard().summary().sevenDays())
                @template.components.rainfallCard(key = "twentyEightDays", label = "28 days", result = view.dashboard().summary().twentyEightDays())
                @template.components.rainfallCard(key = "monthToDate", label = "Month to date", result = view.dashboard().summary().monthToDate())
                @template.components.rainfallCard(key = "yearToDate", label = "Year to date", result = view.dashboard().summary().yearToDate())
              </div>
              <div class="row row-cards mt-1">
                <div class="col-lg-8">
                  <article class="card h-100">
                    <div class="card-header"><h3 class="card-title">Cumulative rainfall</h3></div>
                    <div class="card-body"><div id="cumulative-chart" class="rainfall-chart" role="img" aria-label="Cumulative rainfall chart"></div></div>
                  </article>
                </div>
                <div class="col-lg-4">
                  <article class="card h-100">
                    <div class="card-header"><h3 class="card-title">Data quality</h3></div>
                    <div class="card-body" id="quality-panel">
                      <div class="mb-3">@template.components.qualityBadge(status = view.dashboard().status())</div>
                      <dl class="quality-grid mb-0">
                        <div><dt>Completeness</dt><dd data-quality-completeness>${view.dashboard().quality().completenessPercentage()}%</dd></div>
                        <div><dt>Received</dt><dd data-quality-received>${view.dashboard().quality().receivedSamples()}</dd></div>
                        <div><dt>Expected</dt><dd data-quality-expected>${view.dashboard().quality().expectedSamples()}</dd></div>
                        <div><dt>Longest gap</dt><dd data-quality-gap>${view.dashboard().quality().longestGap().toString()}</dd></div>
                        <div><dt>Unresolved resets</dt><dd data-quality-resets>${view.dashboard().quality().unresolvedResetCount()}</dd></div>
                        <div><dt>Conflicts</dt><dd data-quality-conflicts>${view.dashboard().quality().conflictingObservationCount()}</dd></div>
                      </dl>
                    </div>
                  </article>
                </div>
                <div class="col-lg-7">
                  <article class="card">
                    <div class="card-header"><h3 class="card-title">Daily rainfall</h3></div>
                    <div class="card-body"><div id="daily-chart" class="rainfall-chart" role="img" aria-label="Daily rainfall bar chart"></div></div>
                    @template.components.rainfallTable(days = view.dashboard().dailyRainfall())
                  </article>
                </div>
                <div class="col-lg-5">
                  <article class="card mb-3">
                    <div class="card-header"><h3 class="card-title">Native interval increments</h3></div>
                    <div class="card-body"><div id="increment-chart" class="rainfall-chart compact" role="img" aria-label="Native interval rainfall increments"></div></div>
                  </article>
                  <article class="card">
                    <div class="card-header"><h3 class="card-title">Observation source</h3></div>
                    <div class="card-body">@template.components.sourceStatus(source = view.dashboard().source())</div>
                  </article>
                </div>
                <div class="col-12">
                  <article class="card">
                    <div class="card-header"><h3 class="card-title">Warnings and source validation</h3></div>
                    <div class="card-body">
                      <ul id="dashboard-warnings" class="warning-list">
                        @if (view.dashboard().warnings().isEmpty())
                          <li class="text-secondary">No calculation warnings for this result.</li>
                        @endif
                        @for (String warning : view.dashboard().warnings())
                          <li>${warning}</li>
                        @endfor
                      </ul>
                      <div id="source-discrepancies">
                        @for (DashboardResponse.SourceDiscrepancy discrepancy : view.dashboard().discrepancies())
                          <div class="alert alert-warning py-2">
                            ${discrepancy.date().toString()}: native calculation ${discrepancy.calculatedInches()} in;
                            ${discrepancy.validationSource()} ${discrepancy.validationInches()} in.
                            Neither value was averaged or substituted.
                          </div>
                        @endfor
                      </div>
                    </div>
                  </article>
                </div>
              </div>
            @else
              <div class="card mt-3"><div class="card-body empty-state">
                <h3>Rainfall data are not available right now</h3>
                <p class="text-secondary">Station metadata remains available. Try again after the provider recovers.</p>
              </div></div>
            @endif
            @if (!view.dailyStation())
              <div class="alert alert-info mt-3" role="note">
              <strong>Provisional data.</strong> Missing intervals are never treated as zero. Displaying 0.1 mm does not imply 0.1 mm sensor resolution; 0.01 in equals 0.254 mm.
              </div>
            @endif
          </section>
        @endif
      </div>
    </div>
    <script src="/vendor/leaflet/leaflet.js"></script>
  `
)
