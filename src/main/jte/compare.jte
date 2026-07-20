@import com.pembana.raingauge.dashboard.ComparisonView
@import com.pembana.raingauge.dashboard.ComparisonResponse
@import com.pembana.raingauge.station.StationResponse
@import com.pembana.raingauge.support.PageMetadata

@param ComparisonView view
@param PageMetadata pageMetadata

@template.layout.page(
  title = "Compare stations — Pembana Rain Gauge",
  active = "compare",
  scriptModule = "/js/comparison.js",
  pageMetadata = pageMetadata,
  content = @`
    <div class="page-header"><div class="container-xl py-4"><div class="page-pretitle">Common-period analysis</div><h1 class="page-title">Compare rainfall stations</h1><p class="text-secondary mb-0">Nearby stations can experience different microclimates; comparison does not make them interchangeable.</p></div></div>
    <div class="page-body"><div class="container-xl">
      <form method="get" action="/compare" class="card card-body mb-3" id="comparison-form">
        <fieldset><legend class="form-label">Select up to eight stations</legend><div class="station-check-grid">
          @for (StationResponse station : view.availableStations())
            <label class="form-check"><input class="form-check-input" type="checkbox" name="station" value="${station.stationId()}" checked="${view.selectedStationIds().contains(station.stationId())}"><span class="form-check-label">${station.selectionLabel()}</span></label>
          @endfor
        </div></fieldset>
        <div class="row g-2 mt-2"><div class="col-md-3"><label class="form-label" for="compare-period">Period</label><select class="form-select" id="compare-period" name="period"><option value="24h">24 hours</option><option value="7d">7 days</option><option value="28d" selected>28 days</option><option value="mtd">Month to date</option><option value="ytd">Year to date</option></select></div><div class="col-md-3"><label class="form-label" for="compare-unit">Unit</label><select class="form-select" id="compare-unit" name="unit"><option value="imperial">Inches</option><option value="metric">Millimeters</option></select></div><div class="col-md-auto d-flex align-items-end"><button class="btn btn-primary" type="submit">Compare stations</button></div></div>
      </form>
      @if(view.error() != null)
        @template.components.alert(tone = "danger", title = "Comparison unavailable", message = view.error())
      @endif
      @if(view.comparison() != null)
        <div class="row row-cards"><div class="col-12"><article class="card"><div class="card-header"><h2 class="card-title">Daily rainfall bars</h2></div><div class="card-body"><div id="comparison-chart" class="rainfall-chart large"></div></div></article></div>
        <div class="col-12"><article class="card"><div class="card-header"><h2 class="card-title">Cumulative rainfall</h2></div><div class="card-body"><div id="comparison-cumulative-chart" class="rainfall-chart large"></div></div></article></div>
        <div class="col-12"><article class="card"><div class="table-responsive"><table class="table table-vcenter card-table"><thead><tr><th>Station</th><th>Total</th><th>Status</th><th>Completeness</th><th>Latest observation</th></tr></thead><tbody>
          @for (ComparisonResponse.ComparisonStation station : view.comparison().stations())
            <tr><td><a href="/stations/${station.station().stationId()}">${station.station().selectionLabel()}</a><div class="text-secondary">${station.station().island()} · ${station.station().region()}</div></td><td>${station.total().display()}</td><td>@template.components.qualityBadge(status = station.total().status())</td><td>${station.total().completeness()}%</td><td>${station.total().observationCutoff() == null ? "—" : station.total().observationCutoff().toString()}</td></tr>
          @endfor
        </tbody></table></div></article></div></div>
      @else
        <div class="card"><div class="card-body empty-state"><h2>Select stations to compare</h2><p class="text-secondary">The server-rendered form works without JavaScript.</p></div></div>
      @endif
    </div></div>
  `
)
