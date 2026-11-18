@import com.pembana.raingauge.cocorahs.DailyReportsResponse
@import com.pembana.raingauge.dashboard.ReportDates
@import com.pembana.raingauge.rainfall.RainfallResultStatus

@param DailyReportsResponse reports
@param ReportDates dates

<section class="card mt-3 mb-3" aria-label="CoCoRaHS rainfall summary">
  <div class="card-body report-summary">
    <div>
      <h3 class="subheader">Reported rainfall</h3>
      <div class="d-flex flex-wrap align-items-center gap-2">
        <strong class="h1 rainfall-value mb-0">${reports.coverage().expectedDays() == 0 ? "Pending" : reports.display()}</strong>
        @if (reports.status() != RainfallResultStatus.UNAVAILABLE)
          @template.components.qualityBadge(status = reports.status())
        @endif
      </div>
      <div class="text-secondary mt-1">${dates.display()} · observation dates</div>
    </div>
    <div>
      <h3 class="subheader">Report coverage</h3>
      @if (reports.coverage().expectedDays() == 0)
        <strong class="h2">Awaiting today’s report</strong>
      @else
        <strong class="h2">${reports.coverage().coveredDays()} of ${reports.coverage().expectedDays()} dates</strong>
      @endif
      <div class="text-secondary mt-1">
        Totals include daily readings and whole multi-day reports.
        @if (reports.coverage().expectedDays() > 0 && reports.coverage().expectedDays() < reports.days().size())
          Today’s reading is pending.
        @endif
      </div>
    </div>
  </div>
</section>
@if (reports.source().staleCache() || reports.status() == RainfallResultStatus.CONFLICTING)
  <ul class="alert alert-warning ps-5" aria-label="Report notes">
    @for (String warning : reports.warnings())
      <li>${warning}</li>
    @endfor
  </ul>
@endif
@template.components.reportSeries(reports = reports, canRefresh = false, compact = true)
<p class="text-secondary small">Rainfall reports from <a href="https://www.cocorahs.org/Content.aspx?page=datausagepolicy">CoCoRaHS (CC BY 4.0)</a>. Station locations from the Iowa Environmental Mesonet.</p>
