@import com.pembana.raingauge.dashboard.DashboardResponse

@param String key
@param String label
@param DashboardResponse.Result result

<article class="col-sm-6 col-lg rainfall-summary-card" data-summary-card="${key}">
  <div class="card h-100">
    <div class="card-body">
      <div class="d-flex align-items-center justify-content-between gap-2">
        <div class="subheader">${label}</div>
        <button type="button" class="quality-badge-trigger" data-bs-toggle="modal" data-bs-target="#quality-${key}" aria-label="Show quality details for ${label}">@template.components.qualityBadge(status = result.status())</button>
      </div>
      <div class="h1 mb-1 rainfall-value" data-summary-value>${result.display()}</div>
      <div class="text-secondary small"><span data-summary-completeness>${result.completeness()}%</span> expected samples</div>
    </div>
  </div>
  <div class="modal fade" id="quality-${key}" tabindex="-1" aria-labelledby="quality-title-${key}" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable"><div class="modal-content">
      <div class="modal-header"><div><div class="page-pretitle">Rainfall quality checks</div><h2 class="modal-title" id="quality-title-${key}">${label}</h2></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
      <div class="modal-body"><p class="text-secondary">Completeness measures observation count. The overall status also checks the baseline, time gaps, resets, duplicate conflicts, and outliers.</p><div class="list-group list-group-flush" data-quality-conditions>
        @for (DashboardResponse.QualityCondition condition : result.qualityConditions())
          <div class="list-group-item px-0"><div class="d-flex gap-3"><span class="badge ${condition.passed() ? "bg-green-lt text-green" : "bg-red-lt text-red"}">${condition.passed() ? "Passed" : "Failed"}</span><div><div class="fw-semibold">${condition.label()}</div><div class="text-secondary small mt-1">${condition.detail()}</div></div></div></div>
        @endfor
      </div></div>
      <div class="modal-footer"><button type="button" class="btn" data-bs-dismiss="modal">Close</button></div>
    </div></div>
  </div>
</article>
