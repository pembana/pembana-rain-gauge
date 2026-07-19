import './site.js';

export function comparisonApiUrl(stations, period, unit) {
  const parameters = new URLSearchParams({ period, unit });
  for (const station of stations) parameters.append('station', station);
  return `/api/compare?${parameters.toString()}`;
}

export function comparisonSeries(response) {
  return response.stations.map((entry) => ({
    name: entry.station.displayName,
    type: 'bar',
    data: entry.dailyRainfall.map((day) => day.value),
    emphasis: { focus: 'series' }
  }));
}

export function cumulativeComparisonSeries(response) {
  return response.stations.map((entry) => ({
    name: entry.station.displayName,
    type: 'line',
    connectNulls: false,
    showSymbol: entry.cumulativeRainfall.length < 80,
    data: entry.cumulativeRainfall.map((point) => ({
      value: [point.timestamp, point.value],
      raw: point
    })),
    emphasis: { focus: 'series' }
  }));
}

async function initializeComparison() {
  const chartElement = document.querySelector('#comparison-chart');
  const cumulativeElement = document.querySelector('#comparison-cumulative-chart');
  const form = document.querySelector('#comparison-form');
  if (!chartElement || !cumulativeElement || !form || !window.echarts) return;
  const chart = window.echarts.init(chartElement);
  const cumulativeChart = window.echarts.init(cumulativeElement);
  const data = new FormData(form);
  const stations = data.getAll('station');
  if (!stations.length) return;
  chart.showLoading();
  cumulativeChart.showLoading();
  try {
    const response = await fetch(comparisonApiUrl(stations, data.get('period'), data.get('unit')), {
      headers: { Accept: 'application/json' }
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const comparison = await response.json();
    const dates = comparison.stations[0]?.dailyRainfall.map((day) => day.date) || [];
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { type: 'scroll' },
      grid: { left: 50, right: 20, top: 55, bottom: 65 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', min: 0, name: comparison.unit === 'metric' ? 'mm' : 'in' },
      dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 10 }],
      series: comparisonSeries(comparison)
    });
    cumulativeChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { type: 'scroll' },
      grid: { left: 50, right: 20, top: 55, bottom: 65 },
      xAxis: { type: 'time' },
      yAxis: { type: 'value', min: 0, name: comparison.unit === 'metric' ? 'mm' : 'in' },
      dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 10 }],
      series: cumulativeComparisonSeries(comparison)
    });
  } finally {
    chart.hideLoading();
    cumulativeChart.hideLoading();
  }
  window.addEventListener('resize', () => {
    chart.resize();
    cumulativeChart.resize();
  });
}

if (typeof document !== 'undefined') initializeComparison();
