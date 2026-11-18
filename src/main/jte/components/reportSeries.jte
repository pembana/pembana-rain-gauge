@import com.pembana.raingauge.cocorahs.DailyReport
@import com.pembana.raingauge.cocorahs.DailyReportsResponse

@param DailyReportsResponse reports
@param boolean canRefresh
@param boolean compact = false

<section class="card mb-3">
  <div class="card-header d-flex flex-wrap gap-2 justify-content-between">
    <h2 class="card-title">Daily CoCoRaHS reports</h2>
    @if (canRefresh)
      <button form="daily-report-refresh" class="btn btn-sm btn-outline-primary" type="submit"
        data-loading-message="Checking CoCoRaHS…">Refresh displayed series</button>
    @endif
  </div>
  @if (reports.coverage().dailyReportDays() > 0)
    <div class="card-body pb-1">
      <p class="text-secondary mb-1">A daily total runs from the previous gauge reading to the reading time shown in HST.</p>
      <div id="cocorahs-daily-chart" class="rainfall-chart" data-unit="${reports.unit()}" hidden aria-hidden="true"></div>
    </div>
  @else
    <p class="text-secondary p-3 mb-0">No individual daily amounts are available for these dates.</p>
  @endif
  <details class="report-entries" open="${!compact}">
    <summary class="px-3 py-2 fw-medium">Observation dates (${reports.days().size()})</summary>
    <div class="table-responsive">
      <table id="cocorahs-daily-reports" class="table table-vcenter card-table">
        <thead><tr><th scope="col">Observation date</th><th scope="col">Reading time (HST)</th><th scope="col">Precipitation</th></tr></thead>
        <tbody>
          @for (DailyReportsResponse.Day day : (compact ? reports.days().reversed() : reports.days()))
            !{DailyReport report = day.report();}
            <tr data-date="${day.date().toString()}" data-value="${day.value()}" data-label="${day.display()}" data-time="${report == null ? "" : report.observedAt().toLocalTime().toString()}">
              <th scope="row">${day.date().toString()}</th>
              <td>${report == null ? "—" : report.observedAt().toLocalTime().toString()}</td>
              <td>
                ${day.display()}
                @if (canRefresh && ("Missing".equals(day.display()) || "Pending".equals(day.display())))
                  <button type="submit" form="daily-report-refresh" name="date" value="${day.date().toString()}"
                    class="btn btn-sm btn-ghost-primary ms-1" aria-label="Check for update on ${day.date().toString()}"
                    data-loading-message="Checking CoCoRaHS…">Check for update</button>
                @endif
              </td>
            </tr>
          @endfor
        </tbody>
      </table>
    </div>
  </details>
</section>
@if (!reports.spans().isEmpty())
  <section class="card mb-3">
    <div class="card-header"><h2 class="card-title">Multi-day accumulations</h2></div>
    <div class="table-responsive">
      <table class="table table-vcenter card-table">
        <thead><tr><th scope="col">Observation dates</th><th scope="col">Reading time (HST)</th><th scope="col">Precipitation</th><th scope="col">Treatment</th></tr></thead>
        <tbody>
          @for (DailyReportsResponse.Span span : reports.spans())
            <tr><th scope="row">${span.report().start().toString()} to ${span.report().end().toString()}</th>
              <td>${span.report().observedAt().toLocalTime().toString()}</td><td>${span.display()}</td><td>${span.reason()}</td></tr>
          @endfor
        </tbody>
      </table>
    </div>
  </section>
@endif
