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

## Follow-ups

- [ ] Verify the filtered production station list after the next deployment.
