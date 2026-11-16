import assert from 'node:assert/strict';
import test from 'node:test';
import { dailyReportChart } from '../../main/resources/static/js/cocorahs-station.js';
import { cumulativeYearSeries } from '../../main/resources/static/js/year-comparison.js';

test('daily bars distinguish explicit zero, missing, and trace', () => {
  const rows = [
    { date: '2024-02-01', value: '0', label: '0 in' },
    { date: '2024-02-02', value: '', label: 'Missing' },
    { date: '2024-02-03', value: '', label: 'Trace' }
  ];
  const chart = dailyReportChart(rows, 'imperial');
  assert.deepEqual(chart.series[0].data, [0, null, null]);
  assert.deepEqual(chart.series[1].data, [null, null, 0]);
});

test('annual accumulation annotates the multi-day amount only on its ending date', () => {
  const [line] = cumulativeYearSeries([{
    year: 2024,
    reportDetails: {},
    cumulative: [
      { date: '2024-01-31', value: null },
      { date: '2024-02-01', value: 0.35, annotation: 'Multi-day report 2024-01-30 to 2024-02-01' }
    ]
  }]);
  assert.deepEqual(line.data, [['01-31', null], ['02-01', 0.35]]);
  assert.deepEqual(line.markPoint.data.map((marker) => marker.coord), [['02-01', 0.35]]);
});
