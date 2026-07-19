@param String active

<header class="navbar navbar-expand-md d-print-none sticky-top app-navbar">
  <div class="container-xl">
    <a class="navbar-brand navbar-brand-autodark" href="/" aria-label="Pembana Rain Gauge home">
      <span class="brand-mark" aria-hidden="true">
        <svg width="28" height="28" viewBox="0 0 28 28" role="img">
          <path d="M14 2C9 9 5 13 5 18a9 9 0 0 0 18 0c0-5-4-9-9-16Z" fill="currentColor"/>
          <path d="M9 19c1 3 3 4 6 4" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </span>
      <span><strong>Pembana</strong> <span class="product-name">Rain Gauge</span></span>
    </a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbar-menu"
            aria-controls="navbar-menu" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbar-menu">
      <nav class="navbar-nav ms-auto" aria-label="Primary navigation">
        <a class="nav-link ${active.equals("dashboard") ? "active" : ""}" href="/">
          <svg class="icon-tabler" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 13a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-6M15 9a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v10a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-10M9 5a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v14a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-14M4 20h14"/></svg>
          Dashboard
        </a>
        <a class="nav-link ${active.equals("stations") ? "active" : ""}" href="/stations">
          <svg class="icon-tabler" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0"/><path d="M17.657 16.657l-4.243 4.243a2 2 0 0 1 -2.827 0l-4.244 -4.243a8 8 0 1 1 11.314 0"/></svg>
          Stations
        </a>
        <a class="nav-link ${active.equals("compare") ? "active" : ""}" href="/compare">
          <svg class="icon-tabler" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19l4 -6l4 2l4 -5l4 4"/><path d="M4 5v14h16"/></svg>
          Compare
        </a>
        <a class="nav-link ${active.equals("about") ? "active" : ""}" href="/about-data">
          <svg class="icon-tabler" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0"/><path d="M12 9h.01M11 12h1v4h1"/></svg>
          About data
        </a>
        <button class="nav-link theme-toggle" type="button" id="theme-toggle" aria-label="Toggle color theme">
          <svg class="icon-tabler" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3h.393a7.5 7.5 0 0 0 7.92 12.446a9 9 0 1 1 -8.313 -12.454z"/></svg>
          Theme
        </button>
      </nav>
    </div>
  </div>
</header>
