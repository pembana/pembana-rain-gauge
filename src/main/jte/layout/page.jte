@import gg.jte.Content
@import com.pembana.raingauge.support.PageMetadata
@import org.jspecify.annotations.Nullable

@param String title
@param String active
@param @Nullable String scriptModule
@param PageMetadata pageMetadata
@param Content content

<!doctype html>
<html lang="en" data-bs-theme="light">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Near-real-time and historical Hawaiʻi rainfall from native station observations">
  <meta name="color-scheme" content="light dark">
  <title>${title}</title>
  <link rel="canonical" href="${pageMetadata.canonicalUrl()}">
  <meta property="og:type" content="website">
  <meta property="og:locale" content="en_US">
  <meta property="og:site_name" content="Pembana Rain Gauge">
  <meta property="og:title" content="${title}">
  <meta property="og:description" content="Near-real-time and historical Hawaiʻi rainfall from native station observations">
  <meta property="og:url" content="${pageMetadata.canonicalUrl()}">
  <meta property="og:image" content="${pageMetadata.socialImageUrl()}">
  <meta property="og:image:type" content="image/png">
  <meta property="og:image:width" content="1200">
  <meta property="og:image:height" content="630">
  <meta property="og:image:alt" content="Pembana Rain Gauge logo">
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${title}">
  <meta name="twitter:description" content="Near-real-time and historical Hawaiʻi rainfall from native station observations">
  <meta name="twitter:image" content="${pageMetadata.socialImageUrl()}">
  <meta name="twitter:image:alt" content="Pembana Rain Gauge logo">
  <link rel="icon" href="/favicon.svg" type="image/svg+xml">
  <link rel="stylesheet" href="/vendor/tabler/tabler.min.css">
  <link rel="stylesheet" href="/css/app.css">
</head>
<body>
  <div class="page">
    @template.components.navbar(active = active)
    <main class="page-wrapper" id="main-content">
      ${content}
    </main>
    <footer class="footer footer-transparent d-print-none">
      <div class="container-xl py-3 d-flex flex-wrap justify-content-between gap-2">
        <span><strong>Pembana</strong> Rain Gauge</span>
        <span class="text-secondary">Provisional environmental data · Pacific/Honolulu</span>
      </div>
    </footer>
  </div>
  <script src="/vendor/echarts/echarts.min.js"></script>
  <script src="/vendor/tabler/tabler.min.js"></script>
  @if (scriptModule != null)
    <script type="module" src="${scriptModule}"></script>
  @endif
</body>
</html>
