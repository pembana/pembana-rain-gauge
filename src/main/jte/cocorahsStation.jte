@import java.util.List
@import com.pembana.raingauge.cocorahs.DailyReportService.RefreshCheck
@import org.springframework.security.web.csrf.CsrfToken
@import com.pembana.raingauge.cocorahs.DailyReport
@import com.pembana.raingauge.cocorahs.DailyReportsResponse
@import com.pembana.raingauge.cocorahs.DailyReportsResponse.Day
@import com.pembana.raingauge.station.StationResponse
@import com.pembana.raingauge.support.PageMetadata
@import org.jspecify.annotations.Nullable

@param StationResponse station
@param String url
@param String from
@param String to
@param String today
@param String unit
@param @Nullable DailyReportsResponse reports
@param String latestDisplay
@param @Nullable String error
@param PageMetadata pageMetadata
@param @Nullable CsrfToken csrf
@param boolean canRefresh
@param @Nullable List<RefreshCheck> refreshChecks
@param @Nullable Day refreshedReport

@template.layout.page(
  title = station.selectionLabel() + " — Pembana Rain Gauge",
  active = "stations",
  scriptModule = "/js/cocorahs-station.js",
  pageMetadata = pageMetadata,
  content = @`
    <div class="page-header"><div class="container-xl py-4">
      <div class="page-pretitle">CoCoRaHS · Daily reports</div>
      <h1 class="page-title">${station.selectionLabel()}</h1>
      <p class="text-secondary mt-2 mb-2">Volunteer gauge readings describe precipitation preceding the observation time in Hawaiʻi.</p>
      <div class="d-flex flex-wrap gap-3">
        <a href="/?station=${station.stationId()}&amp;unit=${unit}">Station dashboard</a>
        <a href="/stations?network=HI_COCORAHS">Browse CoCoRaHS stations</a>
        <a href="/compare/years?station=${station.stationId()}&amp;unit=${unit}">Compare years</a>
      </div>
    </div></div>
    <div class="page-body"><div class="container-xl">
      <form class="card card-body mb-3" method="get" action="${url}">
        <div class="row g-3 align-items-end">
          <div class="col-sm">
            <label class="form-label" for="report-from">First observation date</label>
            <input class="form-control" type="date" id="report-from" name="from" value="${from}" max="${today}" required>
          </div>
          <div class="col-sm">
            <label class="form-label" for="report-to">Last observation date</label>
            <input class="form-control" type="date" id="report-to" name="to" value="${to}" max="${today}" required>
          </div>
          <div class="col-sm">
            <label class="form-label" for="report-unit">Unit</label>
            <select class="form-select" id="report-unit" name="unit">
              <option value="metric" selected="${unit.equals("metric")}">Millimeters</option>
              <option value="imperial" selected="${unit.equals("imperial")}">Inches</option>
            </select>
          </div>
          <div class="col-sm-auto"><button class="btn btn-primary" type="submit" data-loading-message="Loading reports…">Show reports</button></div>
        </div>
      </form>
      @template.components.cocorahsRefreshStatus(checks = refreshChecks, day = refreshedReport)
      @if (csrf != null && canRefresh)
        <form id="daily-report-refresh" method="post" action="${url}/refresh-reports">
          <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
          <input type="hidden" name="from" value="${from}">
          <input type="hidden" name="to" value="${to}">
          <input type="hidden" name="unit" value="${unit}">
        </form>
      @endif
      @if (error != null)
        @template.components.alert(tone = "danger", title = "Reports unavailable", message = error)
        @if (csrf != null && canRefresh)
          <button form="daily-report-refresh" class="btn btn-outline-primary mb-3" type="submit"
            data-loading-message="Checking CoCoRaHS…">Refresh displayed series</button>
        @endif
      @elseif (reports != null)
        <div class="row row-cards mb-3">
          <div class="col-md-6"><section class="card card-body h-100">
            <h2 class="card-title">Latest report</h2>
            !{DailyReport latest = reports.latestReport();}
            @if (latest != null)
              <div class="h1 mb-1">${latestDisplay}</div>
              <div class="text-secondary">${latest.observedAt().toString().replace('T', ' ')} HST</div>
              @if (latest.multiDay())
                <div class="text-secondary">Multi-day report covering ${latest.start().toString()} to ${latest.end().toString()}</div>
              @endif
            @else
              <p class="text-secondary mb-0">No latest report available.</p>
            @endif
          </section></div>
          <div class="col-md-6"><section class="card card-body h-100">
            <h2 class="card-title">Reported total</h2>
            <div class="h1 mb-1">${reports.display()}</div>
            <div>@template.components.qualityBadge(status = reports.status())</div>
            <div class="text-secondary mt-2">${reports.coverage().coveredDays()} of ${reports.coverage().expectedDays()} observation dates covered, including ${reports.coverage().dailyReportDays()} usable daily reports.</div>
          </section></div>
        </div>
        @if (!reports.warnings().isEmpty())
          <ul class="alert alert-warning ps-5" aria-label="Data notes">
            @for (String warning : reports.warnings())
              <li>${warning}</li>
            @endfor
          </ul>
        @endif
        @template.components.reportSeries(reports = reports, canRefresh = csrf != null && canRefresh)
      @endif
      <p class="text-secondary">Reports from <a href="https://www.cocorahs.org/Content.aspx?page=datausagepolicy">CoCoRaHS (CC BY 4.0)</a>. Station metadata from <a href="https://mesonet.agron.iastate.edu/cocorahs/">Iowa Environmental Mesonet</a>.</p>
    </div></div>
  `
)
