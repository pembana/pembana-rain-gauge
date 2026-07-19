@param String title
@param String message

@template.layout.page(
  title = title + " — Pembana Rain Gauge",
  active = "",
  scriptModule = "/js/site.js",
  content = @`
    <div class="page-body"><div class="container-xl py-5"><div class="card"><div class="card-body empty-state"><div class="empty-state-icon">!</div><h1>${title}</h1><p class="text-secondary">${message}</p><a class="btn btn-primary" href="/">Return to dashboard</a></div></div></div></div>
  `
)
