@import java.util.List
@import com.pembana.raingauge.cocorahs.DailyReportService.RefreshCheck
@import com.pembana.raingauge.cocorahs.DailyReportsResponse.Day
@import org.jspecify.annotations.Nullable

@param @Nullable List<RefreshCheck> checks
@param @Nullable Day day

@if (checks != null)
  <div class="alert alert-info mb-3" role="status">
    @if (checks.isEmpty())
      No archive years are available for the requested dates.
    @else
      <ul class="mb-1 ps-3">
        @for (RefreshCheck check : checks)
          <li>
            <strong>${check.year()}</strong> · Checked ${check.localTime()}.
            @if (!check.succeeded())
              The CoCoRaHS check failed. Previously fetched reports are shown when available.
            @endif
          </li>
        @endfor
      </ul>
      @if (day != null)
        <div class="mb-1">Report for ${day.date().toString()} · <strong>${day.display()}</strong></div>
      @endif
      <div class="text-secondary">Checks for the same station and year share a five-minute cooldown.</div>
    @endif
  </div>
@endif
