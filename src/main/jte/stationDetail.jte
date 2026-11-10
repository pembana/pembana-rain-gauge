@import com.pembana.raingauge.station.StationDetailView
@import com.pembana.raingauge.support.PageMetadata

@param StationDetailView view
@param PageMetadata pageMetadata
@param boolean details
@param String rainfallUrl
@param String detailsUrl

@template.layout.page(
  title = view.station().displayName() + " — Pembana Rain Gauge",
  active = "stations",
  scriptModule = details ? "/js/site.js" : "/js/station-dashboard.js",
  pageMetadata = pageMetadata,
  content = @`
    <div class="page-header"><div class="container-xl py-3">
      <div class="page-pretitle">Station ${view.station().stationId()} · ${view.station().network()}</div>
      <h1 class="page-title">${view.station().selectionLabel()}</h1>
      <p class="text-secondary mb-0">${view.station().sourceName()}</p>
    </div></div>
    <div class="page-body"><div class="container-xl">
      <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
        <nav class="nav nav-tabs flex-grow-1" aria-label="Station views">
          <a class="nav-link ${!details ? "active" : ""}" href="${rainfallUrl}" aria-current="${!details ? "page" : "false"}">Rainfall</a>
          <a class="nav-link ${details ? "active" : ""}" href="${detailsUrl}" aria-current="${details ? "page" : "false"}">Station details</a>
        </nav>
        @if (view.station().precipitationKey() != null)
          <a class="btn btn-sm btn-outline-primary" href="/compare/years?station=${view.station().stationId()}&amp;unit=${view.unit()}" data-loading-message="Loading station years…">Compare years</a>
        @endif
      </div>
      @if (details)
        <article class="card"><div class="card-header"><h2 class="card-title">Station metadata</h2></div><div class="card-body">
          <dl class="quality-grid">
            <div><dt>Station ID</dt><dd>${view.station().stationId()}</dd></div><div><dt>Alias</dt><dd>${view.station().alias()}</dd></div>
            <div><dt>Display name</dt><dd>${view.station().displayName()}</dd></div><div><dt>Source name</dt><dd>${view.station().sourceName()}</dd></div>
            <div><dt>Island</dt><dd>${view.station().island()}</dd></div><div><dt>Region</dt><dd>${view.station().region()}</dd></div>
            <div><dt>Coordinates</dt><dd>${view.station().latitude()}, ${view.station().longitude()}</dd></div><div><dt>Elevation</dt><dd>${view.station().elevation()} m</dd></div>
            <div><dt>Online</dt><dd>${view.station().online()}</dd></div><div><dt>Catalog confirmed</dt><dd>${view.station().catalogConfirmed()}</dd></div>
            <div><dt>Rainfall capability</dt><dd>${view.station().rainfallCapability()}</dd></div><div><dt>SHEF key</dt><dd>${view.station().precipitationKey()}</dd></div>
            <div><dt>Archive</dt><dd>${view.station().archiveBegin() == null ? "—" : view.station().archiveBegin().toString()} – ${view.station().archiveEnd() == null ? "present" : view.station().archiveEnd().toString()}</dd></div><div><dt>Override note</dt><dd>${view.station().note()}</dd></div>
          </dl>
        </div></article>
      @else
        <div class="dashboard-region" id="dashboard-region" data-station-id="${view.station().stationId()}"
             data-period="28d" data-unit="${view.unit()}" aria-live="polite" aria-busy="false">
          <div class="row row-cards">
            @if (view.dashboard() != null)
              <div class="col-12"><div class="row row-cards summary-grid station-summary-grid">
                @template.components.rainfallCard(key = "twentyFourHours", label = "24 hours", result = view.dashboard().summary().twentyFourHours())
                @template.components.rainfallCard(key = "sevenDays", label = "7 days", result = view.dashboard().summary().sevenDays())
                @template.components.rainfallCard(key = "twentyEightDays", label = "28 days", result = view.dashboard().summary().twentyEightDays())
                @template.components.rainfallCard(key = "monthToDate", label = "Month to date", result = view.dashboard().summary().monthToDate())
                @template.components.rainfallCard(key = "yearToDate", label = "Year to date", result = view.dashboard().summary().yearToDate())
              </div></div>
            @elseif (view.error() != null && view.from() == null && view.to() == null)
              <div class="col-12">
                @template.components.alert(tone = "warning", title = "Rainfall unavailable", message = view.error())
              </div>
            @endif
            <div class="col-12">
              <details class="card" open="${view.from() != null || view.to() != null}">
                <summary class="card-body py-2 fw-medium">Calculate rainfall for a custom range</summary>
                <div class="card-body border-top">
                  <form method="get" action="/stations/${view.station().stationId()}" class="row g-3">
                    <div class="col-md-6 col-lg-4"><label class="form-label" for="from">From</label><input class="form-control" type="datetime-local" id="from" name="from" value="${view.from()}"></div>
                    <div class="col-md-6 col-lg-4"><label class="form-label" for="to">To</label><input class="form-control" type="datetime-local" id="to" name="to" value="${view.to()}"></div>
                    <div class="col-6 col-lg-2"><label class="form-label" for="custom-unit">Unit</label><select class="form-select" id="custom-unit" name="unit"><option value="imperial" selected="${view.unit().equals("imperial")}">Inches</option><option value="metric" selected="${view.unit().equals("metric")}">Millimeters</option></select></div>
                    <div class="col-6 col-lg-2 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit" data-loading-message="Calculating rainfall…">Calculate</button></div>
                  </form>
                  <p class="text-secondary mt-3 mb-0">Use Hawaiʻi local time and choose a range of up to 366 days. The end time is excluded.</p>
                  @if (view.error() != null && (view.from() != null || view.to() != null))
                    <div class="alert alert-warning mt-3">${view.error()}</div>
                  @endif
                  @if(view.customResult() != null)
                    <div class="custom-result mt-3 p-3 rounded border"><div class="d-flex justify-content-between"><div><div class="subheader">Custom range total</div><div class="h1 mb-0">${view.customResult().displayValue()}</div></div>@template.components.qualityBadge(status = view.customResult().status())</div><div class="text-secondary mt-2">${view.customResult().quality().completenessPercentage()}% complete · cutoff ${view.customResult().observationCutoff() == null ? "unavailable" : view.customResult().observationCutoff().toString()}</div></div>
                  @endif
                </div>
              </details>
            </div>
            @if (view.dashboard() != null)
              <div class="col-lg-7"><article class="card"><div class="card-header"><h2 class="card-title">Daily rainfall</h2></div><div class="card-body"><div id="daily-chart" class="rainfall-chart"></div></div>@template.components.rainfallTable(days = view.dashboard().dailyRainfall())</article></div>
              <div class="col-lg-5"><article class="card"><div class="card-header"><h2 class="card-title">Cumulative rainfall</h2></div><div class="card-body"><div id="cumulative-chart" class="rainfall-chart"></div><div id="increment-chart" class="rainfall-chart compact"></div></div></article></div>
            @endif
          </div>
          <div class="alert alert-info mt-3"><strong>Provisional data.</strong> Missing intervals are not filled or treated as zero.</div>
        </div>
      @endif
    </div></div>
  `
)
