import test from 'node:test';
import assert from 'node:assert/strict';

import {
  buildDashboardUrl,
  chartOption,
  historyUrl,
  isCurrentRequest,
  stationCoordinates,
  qualityPresentation,
  requiredDashboardFields
} from '../../main/resources/static/js/station-dashboard.js';

test('chart unit label is separated from the chart title', () => {
  const option = chartOption('Daily rainfall', [], 'bar', 'in');

  assert.equal(option.grid.left, 68);
  assert.equal(option.yAxis.nameLocation, 'middle');
  assert.equal(option.yAxis.nameGap, 46);
  assert.equal(option.yAxis.nameRotate, 90);
});

test('dashboard URL encodes path and query values', () => {
  assert.equal(
    buildDashboardUrl('WI HH1', 'custom 7d', 'imperial/US'),
    '/api/stations/WI%20HH1/dashboard?period=custom%207d&unit=imperial%2FUS'
  );
});

test('station coordinates are validated against the Hawaii map bounds', () => {
  assert.deepEqual(stationCoordinates(19.6, -155.5), [19.6, -155.5]);
  assert.deepEqual(stationCoordinates('22.08', '-159.5'), [22.08, -159.5]);
  assert.equal(stationCoordinates(40.7128, -74.006), null);
  assert.equal(stationCoordinates(null, null), null);
});

test('history URL preserves the current dashboard selection', () => {
  assert.equal(
    historyUrl('WIHH1', '28d', 'metric'),
    '/?station=WIHH1&period=28d&unit=metric'
  );
});

test('dashboard response validation requires every top-level section', () => {
  const response = {
    station: {},
    selection: {},
    summary: {},
    quality: {},
    dailyRainfall: [],
    charts: {},
    source: {}
  };

  assert.equal(requiredDashboardFields(response), true);
  assert.equal(requiredDashboardFields({ ...response, quality: null }), false);
  assert.equal(requiredDashboardFields(null), false);
});

test('only the newest asynchronous request can update the dashboard', () => {
  assert.equal(isCurrentRequest(4, 4), true);
  assert.equal(isCurrentRequest(3, 4), false);
});

test('quality presentation safely falls back for unknown states', () => {
  assert.deepEqual(qualityPresentation('COMPLETE'), ['Complete', 'bg-green-lt text-green']);
  assert.deepEqual(qualityPresentation('NOT_A_STATUS'), ['Unavailable', 'bg-secondary-lt text-secondary']);
});
