import test from 'node:test';
import assert from 'node:assert/strict';

import {
  comparisonApiUrl,
  comparisonSeries,
  cumulativeComparisonSeries
} from '../../main/resources/static/js/comparison.js';

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
