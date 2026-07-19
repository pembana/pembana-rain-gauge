@import com.pembana.raingauge.rainfall.RainfallResultStatus

@param RainfallResultStatus status

@if (status == RainfallResultStatus.COMPLETE)
  <span class="badge bg-green-lt text-green quality-badge" data-quality-status>Complete</span>
@elseif (status == RainfallResultStatus.PARTIAL)
  <span class="badge bg-yellow-lt text-yellow quality-badge" data-quality-status>Partial</span>
@elseif (status == RainfallResultStatus.STALE)
  <span class="badge bg-orange-lt text-orange quality-badge" data-quality-status>Stale</span>
@elseif (status == RainfallResultStatus.CONFLICTING)
  <span class="badge bg-red-lt text-red quality-badge" data-quality-status>Conflicting</span>
@else
  <span class="badge bg-secondary-lt text-secondary quality-badge" data-quality-status>Unavailable</span>
@endif
