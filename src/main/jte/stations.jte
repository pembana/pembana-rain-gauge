@import com.pembana.raingauge.station.StationListView
@import com.pembana.raingauge.station.StationResponse
@import com.pembana.raingauge.station.RainfallCapability
@import com.pembana.raingauge.support.PageMetadata

@param StationListView view
@param PageMetadata pageMetadata

@template.layout.page(
  title = "Weather stations — Pembana Rain Gauge",
  active = "stations",
  scriptModule = "/js/site.js",
  pageMetadata = pageMetadata,
  content = @`
    <div class="page-header"><div class="container-xl py-4">
      <div class="page-pretitle">HI_DCP catalog</div>
      <h1 class="page-title">Hawaiʻi weather stations</h1>
      <p class="text-secondary mb-0">${view.stations().size()} shown of ${view.totalStations()} preserved catalog records.</p>
    </div></div>
    <div class="page-body"><div class="container-xl">
      <form class="card card-body mb-3" method="get" action="/stations">
        <div class="row g-2 align-items-end">
          <div class="col-md-3"><label class="form-label" for="station-search">Name or station ID</label><input class="form-control" id="station-search" name="query" value="${view.query()}" placeholder="Waiaha or WIHH1"></div>
          <div class="col-md-2"><label class="form-label" for="island-filter">Island</label><input class="form-control" id="island-filter" name="island" value="${view.island()}" placeholder="Hawaiʻi"></div>
          <div class="col-md-2"><label class="form-label" for="capability-filter">Rainfall capability</label><select class="form-select" id="capability-filter" name="capability"><option value="">Any</option>@for (RainfallCapability capability : RainfallCapability.values())<option value="${capability}" selected="${capability == view.capability()}">${capability}</option>@endfor</select></div>
          <div class="col-md-1"><label class="form-label" for="online-filter">Online</label><select class="form-select" id="online-filter" name="online"><option value="">Any</option><option value="true" selected="${Boolean.TRUE.equals(view.online())}">Yes</option><option value="false" selected="${Boolean.FALSE.equals(view.online())}">No</option></select></div>
          <div class="col-md-1"><label class="form-label" for="enabled-filter">Enabled</label><select class="form-select" id="enabled-filter" name="enabled"><option value="">Any</option><option value="true" selected="${Boolean.TRUE.equals(view.enabled())}">Yes</option><option value="false" selected="${Boolean.FALSE.equals(view.enabled())}">No</option></select></div>
          <div class="col-md-2"><label class="form-label" for="recent-filter">Observed in 24h</label><select class="form-select" id="recent-filter" name="recent"><option value="">Any</option><option value="true" selected="${Boolean.TRUE.equals(view.recent())}">Yes</option><option value="false" selected="${Boolean.FALSE.equals(view.recent())}">No</option></select></div>
          <div class="col-md-1"><button class="btn btn-primary w-100" type="submit">Apply</button></div>
        </div>
      </form>
      <div class="card">
        <div class="table-responsive">
          <table class="table table-vcenter table-striped card-table station-table">
            <thead><tr><th>Station</th><th>Names</th><th>Place</th><th>Coordinates</th><th>Status</th><th>Rainfall</th><th>Archive</th><th>Catalog</th></tr></thead>
            <tbody>
              @for (StationResponse station : view.stations())
                <tr>
                  <td><a class="fw-semibold" href="/stations/${station.stationId()}">${station.stationId()}</a><div class="text-secondary">${station.alias()} · ${station.network()}</div></td>
                  <td>${station.displayName()}<div class="text-secondary small">Source: ${station.sourceName()}</div></td>
                  <td>${station.island()}<div class="text-secondary">${station.region()}</div></td>
                  <td><span>${station.latitude()}, ${station.longitude()}</span><div class="text-secondary">${station.elevation()} m</div></td>
                  <td><span class="badge ${station.online() ? "bg-green-lt text-green" : "bg-secondary-lt"}">${station.online() ? "Online" : "Offline"}</span> <span class="badge ${station.enabled() ? "bg-blue-lt text-blue" : "bg-red-lt text-red"}">${station.enabled() ? "Enabled" : "Disabled"}</span> @if(station.featured())<span class="badge bg-purple-lt text-purple">Featured</span>@endif</td>
                  <td>${station.rainfallCapability()}<div class="text-secondary">${station.precipitationKey()}</div></td>
                  <td>${station.archiveBegin() == null ? "—" : station.archiveBegin().toString()} – ${station.archiveEnd() == null ? "present" : station.archiveEnd().toString()}<div class="text-secondary">Latest: ${station.latestObservationAt() == null ? "—" : station.latestObservationAt().toString()}</div></td>
                  <td>${station.catalogRefreshedAt() == null ? "—" : station.catalogRefreshedAt().toString()}<div class="text-secondary">${station.catalogConfirmed() ? "Confirmed" : "Unconfirmed"}</div></td>
                </tr>
              @else
                <tr><td colspan="8" class="empty-state">No stations match these filters.</td></tr>
              @endfor
            </tbody>
          </table>
        </div>
      </div>
    </div></div>
  `
)
