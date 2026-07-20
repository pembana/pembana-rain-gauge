# Project work log

This file tracks completed implementation work and remaining follow-ups.

## Completed

- [x] Add Kamal 2.12 deployment configuration for the production server using the embedded H2 database.
- [x] Install Node.js in the Docker build stage so Maven can execute the JavaScript tests.
- [x] Fail application startup early when the administrator username or password is missing or blank.
- [x] Filter rainfall-facing station selectors and APIs to confirmed accumulator stations while preserving the complete catalog on the station diagnostics page.
- [x] Discover and persist unknown station rainfall capabilities in a bounded background refresh so application health checks do not wait for hundreds of provider requests.
- [x] Restrict HTTP Basic authentication to administrator routes so missing public assets cannot trigger a browser login dialog.
- [x] Isolate administrator security tests from locally configured production credentials.
- [x] Add an accessible Hawaiʻi station-map modal with coordinate-positioned rainfall station pins.
- [x] Replace the illustrative station map with Leaflet and configurable OpenStreetMap tiles.
- [x] Allow the configured map-tile host in the browser security policy and make the map modal full-screen on mobile.
- [x] Configure Kamal-managed Let's Encrypt HTTPS for Cloudflare Full (strict) deployment.
- [x] Add canonical, Open Graph, and Twitter card metadata with a branded social image.
- [x] Separate chart y-axis unit labels from chart titles.
- [x] Prevent the decorative page background from repeating on long pages.
- [x] Calculate partial rainfall totals from the first in-range observation when the starting accumulator baseline is unavailable.
- [x] Upgrade the build and runtime to Java 26 and explicitly use the G1 garbage collector in production.

## Follow-ups

- [ ] Verify the filtered production station list after the next deployment.
