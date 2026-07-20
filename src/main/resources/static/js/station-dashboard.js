import { applyTheme, nextTheme } from './site.js';

const HAWAII_TIME = new Intl.DateTimeFormat('en-US', {
  timeZone: 'Pacific/Honolulu',
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit'
});

export function buildDashboardUrl(stationId, period, unit) {
  const path = `/api/stations/${encodeURIComponent(stationId)}/dashboard`;
  return `${path}?period=${encodeURIComponent(period)}&unit=${encodeURIComponent(unit)}`;
}

export function requiredDashboardFields(value) {
  return Boolean(value && value.station && value.selection && value.summary
    && value.quality && value.dailyRainfall && value.charts && value.source);
}

export function isCurrentRequest(sequence, currentSequence) {
  return sequence === currentSequence;
}

export function historyUrl(stationId, period, unit) {
  const parameters = new URLSearchParams({ station: stationId, period, unit });
  return `/?${parameters.toString()}`;
}

const HAWAII_BOUNDS = Object.freeze({
  north: 22.35,
  south: 18.75,
  west: -160.4,
  east: -154.5
});

export function stationCoordinates(latitude, longitude) {
  const lat = Number(latitude);
  const lon = Number(longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lon)
      || lat < HAWAII_BOUNDS.south || lat > HAWAII_BOUNDS.north
      || lon < HAWAII_BOUNDS.west || lon > HAWAII_BOUNDS.east) {
    return null;
  }
  return [lat, lon];
}

export function qualityPresentation(status) {
  const presentations = {
    COMPLETE: ['Complete', 'bg-green-lt text-green'],
    PARTIAL: ['Partial', 'bg-yellow-lt text-yellow'],
    STALE: ['Stale', 'bg-orange-lt text-orange'],
    CONFLICTING: ['Conflicting', 'bg-red-lt text-red'],
    UNAVAILABLE: ['Unavailable', 'bg-secondary-lt text-secondary']
  };
  return presentations[status] || presentations.UNAVAILABLE;
}

function putText(root, selector, value) {
  const element = root.querySelector(selector);
  if (element) {
    element.textContent = value == null ? '—' : String(value);
  }
}

function makeBadge(status) {
  const [label, classes] = qualityPresentation(status);
  const badge = document.createElement('span');
  badge.className = `badge quality-badge ${classes}`;
  badge.dataset.qualityStatus = '';
  badge.textContent = label;
  return badge;
}

function formatHawaii(timestamp) {
  if (!timestamp) {
    return 'Unavailable';
  }
  return `${HAWAII_TIME.format(new Date(timestamp))} HST`;
}

function chartOption(title, points, type, unit, cumulative = false) {
  const color = type === 'bar' ? '#0f9f8f' : '#1677a3';
  return {
    animationDuration: 350,
    backgroundColor: 'transparent',
    title: { text: title, left: 8, textStyle: { fontSize: 14 } },
    grid: { left: 50, right: 22, top: 48, bottom: 64 },
    tooltip: {
      trigger: 'axis',
      formatter(parameters) {
        const item = parameters[0]?.data?.raw;
        if (!item) return 'Missing observation';
        const when = item.timestamp.includes('T') ? formatHawaii(item.timestamp) : item.timestamp;
        const inches = item.inches == null ? '—' : `${item.inches} in`;
        const millimeters = item.millimeters == null ? '—' : `${item.millimeters} mm`;
        return `${when}<br>${inches}<br>${millimeters}${item.quality ? `<br>${item.quality}` : ''}`;
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: type === 'bar',
      data: points.map((point) => point.timestamp),
      axisLabel: {
        hideOverlap: true,
        formatter(value) {
          return value.includes('T') ? HAWAII_TIME.format(new Date(value)) : value;
        }
      }
    },
    yAxis: { type: 'value', name: unit, min: 0, scale: !cumulative },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 18, bottom: 10 }],
    series: [{
      name: title,
      type,
      connectNulls: false,
      showSymbol: points.length < 80,
      itemStyle: { color },
      lineStyle: { color, width: 2 },
      areaStyle: type === 'line' && cumulative ? { color: 'rgba(22,119,163,.12)' } : undefined,
      data: points.map((point) => ({ value: point.value, raw: point }))
    }]
  };
}

export class StationDashboardController {
  constructor(root = document) {
    this.root = root;
    this.form = root.querySelector('#station-form');
    this.stationSelector = root.querySelector('#station-selector');
    this.periodSelector = root.querySelector('#period-selector');
    this.unitSelector = root.querySelector('#unit-selector');
    this.region = root.querySelector('#dashboard-region');
    this.error = root.querySelector('#dashboard-error');
    this.map = root.querySelector('#station-map');
    this.mapModal = root.querySelector('#station-map-modal');
    this.mapPins = [...root.querySelectorAll('[data-station-map-pin]')];
    this.stationMap = null;
    this.stationMapMarkers = new Map();
    this.requestSequence = 0;
    this.abortController = null;
    this.charts = this.createCharts();
  }

  createCharts() {
    if (!window.echarts) return {};
    const charts = {};
    for (const id of ['cumulative-chart', 'daily-chart', 'increment-chart']) {
      const element = this.root.querySelector(`#${id}`);
      if (element) charts[id] = window.echarts.init(element);
    }
    return charts;
  }

  connect() {
    if (!this.form || !this.stationSelector || !this.region) return;
    this.form.addEventListener('submit', (event) => {
      event.preventDefault();
      this.load(this.stationSelector.value, { push: true });
    });
    this.stationSelector.addEventListener('change', () => {
      this.load(this.stationSelector.value, { push: true });
    });
    this.periodSelector?.addEventListener('change', () => {
      this.load(this.stationSelector.value, { push: true });
    });
    this.unitSelector?.addEventListener('change', () => {
      this.load(this.stationSelector.value, { push: true });
    });
    this.connectStationMap();
    window.addEventListener('popstate', () => this.restoreFromLocation());
    window.addEventListener('resize', () => {
      Object.values(this.charts).forEach((chart) => chart.resize());
      this.stationMap?.invalidateSize({ pan: false });
    });
    this.load(this.stationSelector.value, { replace: true });
  }

  connectStationMap() {
    if (!this.map || !this.mapModal) return;
    if (!window.L) {
      putText(this.map, '[data-map-loading]', 'The interactive map could not be loaded.');
      return;
    }
    this.mapModal.addEventListener('shown.bs.modal', () => {
      if (!this.stationMap) this.initializeStationMap();
      this.stationMap?.invalidateSize({ pan: false });
    });
  }

  initializeStationMap() {
    const leaflet = window.L;
    this.map.querySelector('[data-map-loading]')?.remove();
    this.stationMap = leaflet.map(this.map, {
      maxBounds: [[17.5, -162], [23.5, -153]],
      maxBoundsViscosity: 0.65,
      minZoom: 5
    });
    leaflet.tileLayer(this.map.dataset.tileUrl, {
      attribution: this.mapAttribution(),
      maxZoom: 19
    }).addTo(this.stationMap);
    leaflet.control.scale({ imperial: true, metric: true, position: 'bottomleft' })
      .addTo(this.stationMap);
    for (const pin of this.mapPins) {
      const coordinates = stationCoordinates(pin.dataset.latitude, pin.dataset.longitude);
      if (!coordinates) continue;
      const selected = pin.dataset.stationId === this.stationSelector.value;
      const marker = leaflet.marker(coordinates, {
        alt: `Show ${pin.dataset.stationLabel}`,
        icon: this.stationMapIcon(selected),
        keyboard: true,
        riseOnHover: true,
        title: pin.dataset.stationLabel,
        zIndexOffset: selected ? 1000 : 0
      }).addTo(this.stationMap);
      marker.bindPopup(this.stationMapPopup(pin), { minWidth: 190 });
      this.stationMapMarkers.set(pin.dataset.stationId, marker);
    }
    this.stationMap.fitBounds([
      [HAWAII_BOUNDS.south, HAWAII_BOUNDS.west],
      [HAWAII_BOUNDS.north, HAWAII_BOUNDS.east]
    ], { padding: [24, 24] });
  }

  stationMapIcon(selected) {
    const stroke = selected ? '#7f1d1d' : '#c2413b';
    const fill = selected ? '#fecaca' : '#fee2e2';
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" `
      + `fill="${fill}" stroke="${stroke}" stroke-width="2" stroke-linecap="round" `
      + 'stroke-linejoin="round"><path d="M12 21c-4-4.35-6-7.35-6-10a6 6 0 1 1 12 0'
      + 'c0 2.65-2 5.65-6 10z"/><circle cx="12" cy="11" r="2"/></svg>';
    return window.L.icon({
      className: `station-leaflet-marker${selected ? ' is-selected' : ''}`,
      iconAnchor: [14, 38],
      iconUrl: `data:image/svg+xml,${encodeURIComponent(svg)}`,
      iconSize: [28, 38],
      popupAnchor: [0, -34]
    });
  }

  stationMapPopup(pin) {
    const popup = document.createElement('div');
    popup.className = 'station-map-popup';
    const name = document.createElement('strong');
    name.textContent = pin.dataset.stationLabel;
    const location = document.createElement('span');
    location.className = 'text-secondary';
    location.textContent = pin.dataset.stationLocation || 'Hawaiʻi';
    const button = document.createElement('button');
    button.className = 'btn btn-primary btn-sm mt-2';
    button.type = 'button';
    button.textContent = 'View station';
    button.addEventListener('click', () => this.selectMapStation(pin.dataset.stationId));
    popup.append(name, location, button);
    return popup;
  }

  mapAttribution() {
    const link = document.createElement('a');
    const fallback = 'https://www.openstreetmap.org/copyright';
    try {
      const configured = new URL(this.map.dataset.attributionUrl, location.href);
      link.href = ['http:', 'https:'].includes(configured.protocol) ? configured.href : fallback;
    } catch {
      link.href = fallback;
    }
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = this.map.dataset.attributionLabel || 'OpenStreetMap';
    return `&copy; ${link.outerHTML} contributors`;
  }

  selectMapStation(stationId) {
    this.stationSelector.value = stationId;
    this.updateStationMapSelection(stationId);
    this.mapModal.querySelector('.btn-close')?.click();
    this.load(stationId, { push: true });
  }

  updateStationMapSelection(stationId) {
    for (const [candidateId, marker] of this.stationMapMarkers) {
      const selected = candidateId === stationId;
      marker.setIcon(this.stationMapIcon(selected));
      marker.setZIndexOffset(selected ? 1000 : 0);
    }
  }

  async load(stationId, historyMode = {}) {
    const period = this.periodSelector?.value || this.region.dataset.period || '28d';
    const unit = this.unitSelector?.value || this.region.dataset.unit || 'imperial';
    this.abortController?.abort();
    this.abortController = new AbortController();
    const sequence = ++this.requestSequence;
    this.setLoading(true);
    this.hideError();
    try {
      const response = await fetch(buildDashboardUrl(stationId, period, unit), {
        headers: { Accept: 'application/json' },
        signal: this.abortController.signal
      });
      if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        throw new Error(problem.detail || `Request failed with HTTP ${response.status}`);
      }
      const dashboard = await response.json();
      if (!requiredDashboardFields(dashboard)) throw new Error('The dashboard response was incomplete');
      if (!isCurrentRequest(sequence, this.requestSequence)) return;
      this.update(dashboard);
      const url = historyUrl(stationId, period, unit);
      if (historyMode.push) history.pushState({ stationId, period, unit }, '', url);
      if (historyMode.replace) history.replaceState({ stationId, period, unit }, '', url);
    } catch (error) {
      if (error.name !== 'AbortError' && isCurrentRequest(sequence, this.requestSequence)) {
        this.showError(error.message);
      }
    } finally {
      if (isCurrentRequest(sequence, this.requestSequence)) this.setLoading(false);
    }
  }

  update(dashboard) {
    const station = dashboard.station;
    putText(this.region, '[data-station-heading]', station.alias
      ? `${station.displayName} (${station.alias})` : `${station.displayName} (${station.stationId})`);
    putText(this.region, '[data-station-location]', [station.island, station.region].filter(Boolean).join(' · '));
    putText(this.region, '[data-station-source-name]', station.sourceName);
    putText(this.region, '[data-station-id]', station.stationId);
    putText(this.region, '[data-observation-cutoff]', formatHawaii(dashboard.observationCutoff));
    document.title = `${station.displayName} rainfall — Pembana Rain Gauge`;
    const summary = dashboard.summary;
    for (const [key, result] of Object.entries(summary)) {
      const card = this.region.querySelector(`[data-summary-card="${key}"]`);
      if (!card) continue;
      putText(card, '[data-summary-value]', result.display);
      putText(card, '[data-summary-completeness]', `${result.completeness}%`);
      const existing = card.querySelector('[data-quality-status]');
      existing?.replaceWith(makeBadge(result.status));
    }
    const qualityStatus = summary.twentyEightDays?.status || 'UNAVAILABLE';
    const qualityBadge = this.region.querySelector('#quality-panel [data-quality-status]');
    qualityBadge?.replaceWith(makeBadge(qualityStatus));
    putText(this.region, '[data-quality-completeness]', `${dashboard.quality.completenessPercentage}%`);
    putText(this.region, '[data-quality-received]', dashboard.quality.receivedSamples);
    putText(this.region, '[data-quality-expected]', dashboard.quality.expectedSamples);
    putText(this.region, '[data-quality-gap]', dashboard.quality.longestGap);
    putText(this.region, '[data-quality-resets]', dashboard.quality.unresolvedResetCount);
    putText(this.region, '[data-quality-conflicts]', dashboard.quality.conflictingObservationCount);
    putText(this.region, '[data-source-provider]', dashboard.source.provider);
    putText(this.region, '[data-source-fetched]', formatHawaii(dashboard.source.fetchedAt));
    putText(this.region, '[data-source-cadence]', dashboard.source.nativeCadence);
    putText(this.region, '[data-source-resolution]', `${dashboard.source.sourceResolution} ${dashboard.source.nativeUnit}`);
    putText(this.region, '[data-source-cache]', dashboard.source.staleCacheUsed ? 'Stale fallback' : 'Current response');
    this.updateDailyTable(dashboard.dailyRainfall);
    this.updateWarnings(dashboard.warnings, dashboard.discrepancies);
    this.updateCharts(dashboard);
    if (this.stationSelector) this.stationSelector.value = station.stationId;
    this.updateStationMapSelection(station.stationId);
  }

  updateDailyTable(days) {
    const body = this.region.querySelector('#daily-rainfall-table tbody');
    if (!body) return;
    const fragment = document.createDocumentFragment();
    for (const day of days) {
      const row = document.createElement('tr');
      const date = document.createElement('td');
      const value = document.createElement('td');
      const status = document.createElement('td');
      date.textContent = day.date;
      value.textContent = day.display;
      status.append(makeBadge(day.status));
      row.append(date, value, status);
      fragment.append(row);
    }
    body.replaceChildren(fragment);
  }

  updateWarnings(warnings, discrepancies) {
    const list = this.region.querySelector('#dashboard-warnings');
    if (list) {
      const items = warnings.length ? warnings : ['No calculation warnings for this result.'];
      list.replaceChildren(...items.map((warning) => {
        const item = document.createElement('li');
        item.textContent = warning;
        return item;
      }));
    }
    const container = this.region.querySelector('#source-discrepancies');
    if (container) {
      container.replaceChildren(...discrepancies.map((difference) => {
        const alert = document.createElement('div');
        alert.className = 'alert alert-warning py-2';
        alert.textContent = `${difference.date}: native calculation ${difference.calculatedInches} in; `
          + `${difference.validationSource} ${difference.validationInches} in. Neither value was averaged or substituted.`;
        return alert;
      }));
    }
  }

  updateCharts(dashboard) {
    const unit = dashboard.selection.unit === 'metric' ? 'mm' : 'in';
    const settings = [
      ['cumulative-chart', 'Cumulative rainfall', dashboard.charts.cumulative, 'line', true],
      ['daily-chart', 'Daily rainfall', dashboard.charts.daily, 'bar', false],
      ['increment-chart', 'Native interval increments', dashboard.charts.increments, 'bar', false]
    ];
    for (const [id, title, points, type, cumulative] of settings) {
      const chart = this.charts[id];
      if (!chart) continue;
      chart.hideLoading();
      chart.setOption(chartOption(title, points, type, unit, cumulative), { notMerge: false });
    }
  }

  restoreFromLocation() {
    const parameters = new URLSearchParams(location.search);
    const station = parameters.get('station') || this.stationSelector.value;
    const period = parameters.get('period') || '28d';
    const unit = parameters.get('unit') || 'imperial';
    if (this.periodSelector) this.periodSelector.value = period;
    if (this.unitSelector) this.unitSelector.value = unit;
    this.load(station);
  }

  setLoading(loading) {
    this.region.setAttribute('aria-busy', String(loading));
    this.region.classList.toggle('is-loading', loading);
    for (const chart of Object.values(this.charts)) {
      if (loading) chart.showLoading('default', { text: 'Loading rainfall observations…' });
    }
  }

  showError(message) {
    if (!this.error) return;
    this.error.classList.remove('d-none');
    putText(this.error, '[data-error-message]', message);
  }

  hideError() {
    this.error?.classList.add('d-none');
  }
}

if (typeof document !== 'undefined') {
  const controller = new StationDashboardController(document);
  controller.connect();
}

export { applyTheme, nextTheme };
