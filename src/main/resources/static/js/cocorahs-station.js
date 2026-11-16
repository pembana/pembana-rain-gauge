import './site.js';

export function dailyReportChart(rows, unit) {
  const symbol = unit === 'metric' ? 'mm' : 'in';
  return {
    tooltip: {
      trigger: 'axis',
      renderMode: 'richText',
      formatter: ([point]) => point ? `${point.axisValue}\n${rows[point.dataIndex].label}` : ''
    },
    grid: { left: 66, right: 24, top: 35, bottom: 42 },
    xAxis: { type: 'category', data: rows.map((row) => row.date), axisLabel: { hideOverlap: true } },
    yAxis: { type: 'value', min: 0, name: symbol },
    series: [
      { name: 'Daily report', type: 'bar', data: rows.map((row) => row.value === '' ? null : Number(row.value)) },
      { name: 'Trace', type: 'scatter', symbol: 'diamond', data: rows.map((row) => row.label === 'Trace' ? 0 : null) }
    ]
  };
}

if (typeof document !== 'undefined') {
  const element = document.getElementById('cocorahs-daily-chart');
  if (element && window.echarts) {
    const rows = Array.from(document.querySelectorAll('#cocorahs-daily-reports tbody tr'),
      (row) => row.dataset);
    element.hidden = false;
    const chart = window.echarts.init(element);
    chart.setOption(dailyReportChart(rows, element.dataset.unit));
    window.addEventListener('resize', () => chart.resize());
  }
}
