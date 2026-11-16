@import java.time.Month
@import java.time.format.TextStyle
@import java.util.List
@import java.util.Locale
@import com.pembana.raingauge.cocorahs.DailyReportService.RefreshCheck
@import org.springframework.security.web.csrf.CsrfToken
@import com.pembana.raingauge.cocorahs.DailyReportsResponse
@import com.pembana.raingauge.dashboard.YearComparisonResponse
@import com.pembana.raingauge.station.StationType
@import com.pembana.raingauge.station.StationResponse
@import com.pembana.raingauge.support.PageMetadata
@import org.jspecify.annotations.Nullable

@param List<StationResponse> stations
@param String stationId
@param List<Integer> availableYears
@param List<Integer> selectedYears
@param String unit
@param String mode
@param @Nullable YearComparisonResponse comparison
@param String chartData
@param @Nullable String error
@param PageMetadata pageMetadata
@param @Nullable CsrfToken csrf
@param @Nullable List<RefreshCheck> refreshChecks

@template.layout.page(
  title = "Compare years — Pembana Rain Gauge",
  active = "compare",
  scriptModule = "/js/year-comparison.js",
  pageMetadata = pageMetadata,
  content = @`
    <div class="page-header">
      <div class="container-xl py-4">
        <div class="page-pretitle">Historical rainfall</div>
        <h1 class="page-title">Compare years</h1>
        <p class="text-secondary mb-0">See how rainfall at one station changes from year to year.</p>
        <a class="mt-2 d-inline-block" href="/compare">Compare stations</a>
      </div>
    </div>
    <div class="page-body"><div class="container-xl">
      @if (!stations.isEmpty())
        <section class="card card-body mb-3" aria-label="Comparison settings">
          <form method="get" action="/compare/years" class="row g-2 align-items-end mb-3">
            <input type="hidden" name="unit" value="${unit}">
            <input type="hidden" name="mode" value="${mode}">
            <div class="col-sm">
              <label class="form-label" for="years-station">Station</label>
              <select class="form-select" id="years-station" name="station">
                @for (StationResponse station : stations)
                  <option value="${station.stationId()}" selected="${stationId.equals(station.stationId())}">${station.selectionLabel()}${station.type() == StationType.DAILY_REPORTS ? " · CoCoRaHS" : ""}</option>
                @endfor
              </select>
            </div>
            <div class="col-sm-auto"><button class="btn btn-outline-primary" type="submit" data-loading-message="Loading station years…">Load station</button></div>
          </form>
          @if (!availableYears.isEmpty())
            <form method="get" action="/compare/years" class="row g-3">
              <input type="hidden" name="station" value="${stationId}">
              <fieldset class="col-12">
                <legend class="form-label">Select up to eight years</legend>
                <div class="d-flex flex-wrap gap-3">
                  @for (Integer year : availableYears)
                    <label class="form-check mb-0">
                      <input class="form-check-input" type="checkbox" name="year" value="${year}" checked="${selectedYears.contains(year)}">
                      <span class="form-check-label">${year}</span>
                    </label>
                  @endfor
                </div>
              </fieldset>
              <div class="col-sm">
                <label class="form-label" for="years-mode">Period</label>
                <select class="form-select" id="years-mode" name="mode">
                  <option value="calendar" selected="${mode.equals("calendar")}">Calendar years</option>
                  <option value="ytd" selected="${mode.equals("ytd")}">Matching year to date</option>
                </select>
              </div>
              <div class="col-sm">
                <label class="form-label" for="years-unit">Unit</label>
                <select class="form-select" id="years-unit" name="unit">
                  <option value="metric" selected="${unit.equals("metric")}">Millimeters</option>
                  <option value="imperial" selected="${unit.equals("imperial")}">Inches</option>
                </select>
              </div>
              <div class="col-sm-auto d-flex align-items-end"><button class="btn btn-primary" type="submit" data-loading-message="Comparing rainfall by year…">Compare years</button></div>
            </form>
          @endif
        </section>
      @endif

      @template.components.cocorahsRefreshStatus(checks = refreshChecks, day = null)
      @if (error != null)
        @template.components.alert(tone = "danger", title = "Comparison unavailable", message = error)
      @elseif (comparison == null || comparison.years().isEmpty())
        <div class="alert alert-info">No rainfall archive years are available.</div>
      @else
        !{boolean observationDates = comparison.periodBasis().equals("observation-date");}
        <div id="year-comparison-results" data-comparison="${chartData}">
          @if (observationDates)
            @if (csrf != null)
              <form method="post" action="/compare/years/refresh-reports" class="mb-3">
                <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
                <input type="hidden" name="station" value="${stationId}">
                <input type="hidden" name="mode" value="${mode}">
                <input type="hidden" name="unit" value="${unit}">
                @for (Integer year : selectedYears)
                  <input type="hidden" name="year" value="${year}">
                @endfor
                <button class="btn btn-outline-primary" type="submit" data-loading-message="Checking CoCoRaHS…">Refresh displayed series</button>
              </form>
            @endif
            <p class="text-secondary">Periods use Hawaiʻi observation dates. Reports describe precipitation preceding each reading. Multi-day amounts enter the cumulative total on the ending observation date.</p>
          @else
            <p class="text-secondary">Periods use Hawaiʻi local time. The current year ends at the latest comparison cutoff. Partial totals and cumulative lines reflect observed rainfall. A dash marks a month without a calculated value.</p>
          @endif
          <div class="row row-cards">
            <section class="col-12"><article class="card">
              <div class="card-header"><h2 class="card-title">${mode.equals("ytd") ? "Matching year-to-date totals" : "Annual rainfall totals"}</h2></div>
              <div id="annual-rainfall-chart" class="rainfall-chart" hidden aria-hidden="true"></div>
              <div class="table-responsive">
                <table class="table table-vcenter card-table">
                  <thead><tr><th scope="col">Year</th><th scope="col">Rainfall</th><th scope="col">Coverage</th><th scope="col">${observationDates ? "Last observation date in period" : "Period end"}</th></tr></thead>
                  <tbody>
                    @for (YearComparisonResponse.YearResult year : comparison.years())
                      <tr>
                        <th scope="row">${year.year()} @if (year.yearToDate())<span class="d-block text-secondary fw-normal">Year to date</span>@endif</th>
                        <td>${year.display()}</td>
                        <td>
                          @template.components.qualityBadge(status = year.status())
                          @if (year.completeness() != null)
                            <span class="ms-1">${year.completeness()}%</span>
                          @endif
                          !{YearComparisonResponse.ReportDetails details = year.reportDetails();}
                          @if (details != null)
                            <span class="d-block text-secondary">${details.coverage().coveredDays()} of ${details.coverage().expectedDays()} dates covered</span>
                          @endif
                          @if (year.staleCache())
                            <span class="d-block text-secondary">Cached observations</span>
                          @endif
                          @if (!year.warnings().isEmpty())
                            <details class="mt-1"><summary>Data notes</summary>
                              <ul class="mb-0">
                                @for (String warning : year.warnings())
                                  <li>${warning}</li>
                                @endfor
                              </ul>
                            </details>
                          @endif
                        </td>
                        <td>${observationDates
                          ? (year.from().equals(year.to()) ? "—" : year.to().toLocalDate().minusDays(1).toString())
                          : year.to().toString().replace('T', ' ')}</td>
                      </tr>
                    @endfor
                  </tbody>
                </table>
              </div>
            </article></section>
            <section class="col-12"><article class="card">
              <div class="card-header"><h2 class="card-title">Monthly rainfall</h2></div>
              <div id="monthly-rainfall-chart" class="rainfall-chart large" hidden aria-hidden="true"></div>
              <div class="table-responsive">
                <table class="table table-vcenter card-table">
                  <thead><tr><th scope="col">Month</th>
                    @for (YearComparisonResponse.YearResult year : comparison.years())
                      <th scope="col">${year.year()}</th>
                    @endfor
                  </tr></thead>
                  <tbody>
                    @for (Month month : Month.values())
                      <tr><th scope="row">${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}</th>
                        @for (YearComparisonResponse.YearResult year : comparison.years())
                          <td>${year.months().get(month.ordinal()).display()}</td>
                        @endfor
                      </tr>
                    @endfor
                    @if (observationDates)
                      <tr><th scope="row">Unallocated across months</th>
                        @for (YearComparisonResponse.YearResult year : comparison.years())
                          !{YearComparisonResponse.ReportDetails details = year.reportDetails();}
                          <td>${details == null ? "—" : details.unallocatedDisplay()}</td>
                        @endfor
                      </tr>
                    @endif
                  </tbody>
                </table>
              </div>
            </article></section>
            @if (observationDates)
              <section class="col-12"><article class="card">
                <div class="card-header"><h2 class="card-title">Multi-day reports</h2></div>
                <div class="table-responsive">
                  <table class="table table-vcenter card-table">
                    <thead><tr><th scope="col">Year</th><th scope="col">Observation dates</th><th scope="col">Precipitation</th><th scope="col">Treatment</th></tr></thead>
                    <tbody>
                      @for (YearComparisonResponse.YearResult year : comparison.years())
                        !{YearComparisonResponse.ReportDetails details = year.reportDetails();}
                        @if (details != null && !details.spans().isEmpty())
                          @for (DailyReportsResponse.Span span : details.spans())
                            <tr><th scope="row">${year.year()}</th><td>${span.report().start().toString()} to ${span.report().end().toString()}</td><td>${span.display()}</td><td>${span.reason()}</td></tr>
                          @endfor
                        @else
                          <tr><th scope="row">${year.year()}</th><td colspan="3">No multi-day reports available for this period.</td></tr>
                        @endif
                      @endfor
                    </tbody>
                  </table>
                </div>
              </article></section>
            @endif
            <section id="cumulative-year-rainfall" class="col-12" hidden><article class="card">
              <div class="card-header"><h2 class="card-title">Cumulative rainfall by day</h2></div>
              <div id="cumulative-year-rainfall-chart" class="rainfall-chart large" role="img" aria-label="Cumulative observed rainfall for each selected year"></div>
            </article></section>
          </div>
        </div>
        @if (observationDates)
          <p class="text-secondary mt-3">Reports from <a href="https://www.cocorahs.org/Content.aspx?page=datausagepolicy">CoCoRaHS (CC BY 4.0)</a>. Station metadata from <a href="https://mesonet.agron.iastate.edu/cocorahs/">Iowa Environmental Mesonet</a>.</p>
        @endif
      @endif
    </div></div>
  `
)
