import test from 'node:test';
import assert from 'node:assert/strict';

import {
  comparisonApiUrl,
  comparisonSeries,
  cumulativeComparisonSeries
} from '../../main/resources/static/js/comparison.js';
import { cumulativeYearSeries } from '../../main/resources/static/js/year-comparison.js';

test('comparison URL sends repeated station parameters', () => {
  assert.equal(
    comparisonApiUrl(['WIHH1', 'KBSH1'], '28d', 'imperial'),
    '/api/compare?period=28d&unit=imperial&station=WIHH1&station=KBSH1'
  );
});

test('comparison series uses display names and preserves missing daily values', () => {
  const response = {
    stations: [
      {
        station: { displayName: 'Waiaha' },
        dailyRainfall: [{ value: 0.2 }, { value: null }],
        cumulativeRainfall: [
          { timestamp: '2026-07-01T00:00:00Z', value: 0.0 },
          { timestamp: '2026-07-01T00:15:00Z', value: 0.2 }
        ]
      }
    ]
  };

  assert.deepEqual(comparisonSeries(response), [{
    name: 'Waiaha',
    type: 'bar',
    data: [0.2, null],
    emphasis: { focus: 'series' }
  }]);

  assert.deepEqual(cumulativeComparisonSeries(response), [{
    name: 'Waiaha',
    type: 'line',
    connectNulls: false,
    showSymbol: true,
    data: [
      {
        value: ['2026-07-01T00:00:00Z', 0.0],
        raw: { timestamp: '2026-07-01T00:00:00Z', value: 0.0 }
      },
      {
        value: ['2026-07-01T00:15:00Z', 0.2],
        raw: { timestamp: '2026-07-01T00:15:00Z', value: 0.2 }
      }
    ],
    emphasis: { focus: 'series' }
  }]);
});

test('year comparison aligns dates without shifting or filling missing readings', () => {
  const [common, leap] = cumulativeYearSeries([
    { year: 2023, cumulative: [
      { date: '2023-02-28', value: 0 }, { date: '2023-03-01', value: 1 }
    ] },
    { year: 2024, cumulative: [
      { date: '2024-02-28', value: 2 }, { date: '2024-02-29', value: null },
      { date: '2024-03-01', value: 3 }
    ] }
  ]);

  assert.deepEqual(common.data, [['02-28', 0], ['03-01', 1]]);
  assert.deepEqual(leap.data, [['02-28', 2], ['02-29', null], ['03-01', 3]]);
  assert.equal(leap.connectNulls, false);
});
