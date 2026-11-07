@import java.time.Month
@import java.time.format.TextStyle
@import java.util.List
@import java.util.Locale
@import com.pembana.raingauge.dashboard.YearComparisonResponse
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
                  <option value="${station.stationId()}" selected="${stationId.equals(station.stationId())}">${station.selectionLabel()}</option>
                @endfor
              </select>
            </div>
            <div class="col-sm-auto"><button class="btn btn-outline-primary" type="submit">Load station</button></div>
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
              <div class="col-sm-auto d-flex align-items-end"><button class="btn btn-primary" type="submit">Compare years</button></div>
            </form>
          @endif
        </section>
      @endif

      @if (error != null)
        @template.components.alert(tone = "danger", title = "Comparison unavailable", message = error)
      @elseif (comparison == null || comparison.years().isEmpty())
        <div class="alert alert-info">No rainfall archive years are available.</div>
      @else
        <div id="year-comparison-results" data-comparison="${chartData}">
          <p class="text-secondary">Periods use Hawaiʻi local time. The current year ends at the latest comparison cutoff. Partial totals and cumulative lines reflect observed rainfall. A dash marks a month without a calculated value.</p>
          <div class="row row-cards">
            <section class="col-12"><article class="card">
              <div class="card-header"><h2 class="card-title">${mode.equals("ytd") ? "Matching year-to-date totals" : "Annual rainfall totals"}</h2></div>
              <div id="annual-rainfall-chart" class="rainfall-chart" hidden aria-hidden="true"></div>
              <div class="table-responsive">
                <table class="table table-vcenter card-table">
                  <thead><tr><th scope="col">Year</th><th scope="col">Rainfall</th><th scope="col">Coverage</th><th scope="col">Period end</th></tr></thead>
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
                        <td>${year.to().toString().replace('T', ' ')}</td>
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
                  </tbody>
                </table>
              </div>
            </article></section>
            <section id="cumulative-year-rainfall" class="col-12" hidden><article class="card">
              <div class="card-header"><h2 class="card-title">Cumulative rainfall by day</h2></div>
              <div id="cumulative-year-rainfall-chart" class="rainfall-chart large" role="img" aria-label="Cumulative observed rainfall for each selected year"></div>
            </article></section>
          </div>
        </div>
      @endif
    </div></div>
  `
)
