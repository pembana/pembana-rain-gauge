# Vendored browser dependencies

The application serves these pinned production assets locally rather than
loading its JavaScript or CSS from a CDN:

* Tabler Core 1.4.0 (`tabler.min.css`, `tabler.min.js`), MIT License,
  downloaded from the package published as `@tabler/core@1.4.0`.
* Tabler Icons 3.44.0 (selected SVG source icons in `tabler-icons/`), MIT License,
  downloaded from the package published as `@tabler/icons@3.44.0`. The navbar
  inlines these paths so they inherit the active theme color without a runtime request.
* Apache ECharts 6.1.0 (`echarts.min.js`), Apache License 2.0,
  downloaded from the package published as `echarts@6.1.0`.
* Leaflet 1.9.4 (`leaflet.js`, `leaflet.css`, and referenced images), BSD-2-Clause
  License, downloaded from the package published as `leaflet@1.9.4`.

Leaflet requests visible map tiles at runtime from the configured tile provider;
the default is OpenStreetMap's standard raster tile service.

License and notice files are stored beside their corresponding assets.
