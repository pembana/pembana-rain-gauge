import './site.js';

export function dailyReportChart(rows, unit) {
  const symbol = unit === 'metric' ? 'mm' : 'in';
  const values = rows.map((row) => row.value == null || row.value === '' ? null : Number(row.value));
  return {
    color: ['#087c72', '#b36d17', '#426e82'],
    tooltip: {
      trigger: 'axis',
      renderMode: 'richText',
      formatter: ([point]) => {
        if (!point) return '';
        const row = rows[point.dataIndex];
        const reading = row.time ? ` · ${row.time} HST` : '';
        return `${row.date}${reading}\n${row.label}`;
      }
    },
    legend: { bottom: 3, selectedMode: false },
    grid: { left: 60, right: 24, top: 36, bottom: 62 },
    xAxis: { type: 'category', data: rows.map((row) => row.date), axisLabel: { hideOverlap: true } },
    yAxis: { type: 'value', min: 0, name: symbol },
    series: [
      { name: 'Daily report', type: 'bar', data: values },
      { name: 'Trace', type: 'scatter', symbol: 'diamond', clip: false, data: rows.map((row) => row.label === 'Trace' ? 0 : null) },
      { name: 'Zero', type: 'scatter', symbol: 'emptyCircle', clip: false, data: values.map((value) => value === 0 ? 0 : null) }
    ]
  };
}

if (typeof document !== 'undefined') {
  const element = document.getElementById('cocorahs-daily-chart');
  if (element && window.echarts) {
    const rows = Array.from(document.querySelectorAll('#cocorahs-daily-reports tbody tr'),
      (row) => row.dataset);
    rows.sort((first, second) => first.date.localeCompare(second.date));
    element.hidden = false;
    const chart = window.echarts.init(element);
    chart.setOption(dailyReportChart(rows, element.dataset.unit));
    window.addEventListener('resize', () => chart.resize());
  }
}
