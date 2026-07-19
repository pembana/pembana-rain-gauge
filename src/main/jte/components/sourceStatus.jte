@import com.pembana.raingauge.dashboard.DashboardResponse

@param DashboardResponse.Source source

<dl class="source-status mb-0">
  <div><dt>Provider</dt><dd data-source-provider>${source.provider()}</dd></div>
  <div><dt>Fetched</dt><dd data-source-fetched>${source.fetchedAt().toString()}</dd></div>
  <div><dt>Native cadence</dt><dd data-source-cadence>${source.nativeCadence()}</dd></div>
  <div><dt>Resolution</dt><dd data-source-resolution>${source.sourceResolution()} ${source.nativeUnit()}</dd></div>
  <div><dt>Cache</dt><dd data-source-cache>${source.staleCacheUsed() ? "Stale fallback" : "Current response"}</dd></div>
</dl>
