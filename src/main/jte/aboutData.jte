@import com.pembana.raingauge.dashboard.AboutDataView

@param AboutDataView view

@template.layout.page(
  title = "About the data — Pembana Rain Gauge",
  active = "about",
  scriptModule = "/js/site.js",
  content = @`
    <div class="page-header"><div class="container-xl py-4"><div class="page-pretitle">Methods and provenance</div><h1 class="page-title">About the data</h1><p class="text-secondary mb-0">How Pembana Rain Gauge discovers stations, calculates totals, and communicates uncertainty.</p></div></div>
    <div class="page-body"><div class="container-xl"><div class="row row-cards">
      <div class="col-lg-8"><article class="card prose-card"><div class="card-body">
        <h2>Sources</h2><p>The station catalog comes from the Iowa Environmental Mesonet <code>HI_DCP</code> GeoJSON service. Native SHEF observations come from its HADS archive. NOAA HADS metadata is used to verify station decode behavior. IEM daily summaries and NWS rainfall products are validation sources; they never silently replace a native-observation result.</p>
        <h2>Accumulator calculation</h2><p><code>PCIRG</code> means precipitation accumulator in inches. Observations are sorted and deduplicated, a baseline at or before the requested start is selected, and each nonnegative change is added. Repeated values add zero. For WIHH1, NOAA metadata reports 15-minute self-timed <code>PCIRG</code> observations; the application also detects cadence from actual timestamps.</p>
        <h2>Resets, duplicates, and gaps</h2><p>A negative change is not automatically rainfall. A near-zero reset or rollover must be corroborated by surrounding readings and configured thresholds. Unresolved drops make a result partial. Conflicting retransmissions are never averaged. Missing intervals are never treated as zero and rainfall is not interpolated from nearby stations.</p>
        <h2>Status terminology</h2><p><strong>Complete</strong> requires a baseline, sufficient coverage, and no material gap, unresolved reset, or conflicting duplicate. <strong>Partial</strong>, <strong>stale</strong>, <strong>conflicting</strong>, and <strong>unavailable</strong> results state the limitation explicitly. Formatting a value to two decimal places does not make it exact.</p>
        <h2>Time and precision</h2><p>Calendar windows use <code>Pacific/Honolulu</code> and half-open <code>[from, to)</code> boundaries. “Previous 28 days” is exactly 672 hours. Calculations use decimal arithmetic. Inches are displayed to 0.01 in and metric values to 0.1 mm; 0.01 in equals 0.254 mm, so metric formatting does not increase sensor resolution.</p>
        <h2>Caching and provisional data</h2><p>Successful observation responses are cached briefly, with longer caching for immutable historical ranges. Provider errors are not cached as valid empty results. A failed live request can use an explicitly marked stale cached response. NOAA/NWS and IEM observations are provisional and can be corrected.</p>
      </div></article></div>
      <div class="col-lg-4"><article class="card"><div class="card-header"><h2 class="card-title">Build information</h2></div><div class="card-body"><dl class="quality-grid"><div><dt>Application</dt><dd>Pembana Rain Gauge</dd></div><div><dt>Version</dt><dd>${view.applicationVersion()}</dd></div><div><dt>Built</dt><dd>${view.buildTimestamp()}</dd></div><div><dt>Runtime</dt><dd>Java 21 · Spring Boot 4.1</dd></div></dl></div></article></div>
    </div></div></div>
  `
)
