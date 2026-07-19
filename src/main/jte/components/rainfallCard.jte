@import com.pembana.raingauge.dashboard.DashboardResponse

@param String key
@param String label
@param DashboardResponse.Result result

<article class="col-sm-6 col-lg rainfall-summary-card" data-summary-card="${key}">
  <div class="card h-100">
    <div class="card-body">
      <div class="d-flex align-items-start justify-content-between gap-2">
        <div class="subheader">${label}</div>
        @template.components.qualityBadge(status = result.status())
      </div>
      <div class="h1 mb-1 rainfall-value" data-summary-value>${result.display()}</div>
      <div class="text-secondary small"><span data-summary-completeness>${result.completeness()}%</span> expected samples</div>
    </div>
  </div>
</article>
