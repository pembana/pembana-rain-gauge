import './site.js';

const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function dayLabels() {
  const labels = [];
  // A leap-year reference keeps March 1 aligned across all years.
  for (let day = new Date(Date.UTC(2000, 0, 1)); day.getUTCFullYear() === 2000;
    day.setUTCDate(day.getUTCDate() + 1)) {
    labels.push(day.toISOString().slice(5, 10));
  }
  return labels;
}

export function cumulativeYearSeries(years) {
  return years.map((year) => ({
    name: String(year.year),
    type: 'line',
    connectNulls: false,
    showSymbol: false,
    data: year.cumulative.map((point) => [point.date.slice(5), point.value])
  }));
}

function initializeCharts() {
  const results = document.querySelector('#year-comparison-results');
  if (!results || !window.echarts) return;

  const { mode, unit, years } = JSON.parse(results.dataset.comparison);
  const symbol = unit === 'metric' ? 'mm' : 'in';
  const days = dayLabels();
  const charts = [];

  function render(id, labels, series, showLegend = false, daily = false) {
    const element = document.getElementById(id);
    element.hidden = false;
    const chart = window.echarts.init(element);
    chart.setOption({
      tooltip: { trigger: 'axis', valueFormatter: (value) => value == null ? '—' :
        `${Number(value).toFixed(unit === 'metric' ? 1 : 2)} ${symbol}` },
      legend: { show: showLegend, type: 'scroll', top: 8 },
      grid: { left: 66, right: 24, top: showLegend ? 55 : 35, bottom: 42 },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: daily ? {
          interval: (_index, day) => day.endsWith('-01'),
          hideOverlap: true,
          formatter: (day) => months[Number(day.slice(0, 2)) - 1]
        } : {}
      },
      yAxis: { type: 'value', min: 0, name: symbol },
      series
    });
    charts.push(chart);
  }

  render('annual-rainfall-chart',
    years.map((year) => mode === 'calendar' && year.yearToDate ? `${year.year}\nYTD` : String(year.year)),
    [{ name: 'Rainfall', type: 'bar', data: years.map((year) => year.value) }]);

  render('monthly-rainfall-chart', months, years.map((year) => ({
    name: String(year.year), type: 'bar', data: year.months.map((month) => month.value)
  })), true);

  document.getElementById('cumulative-year-rainfall').hidden = false;
  render('cumulative-year-rainfall-chart', days, cumulativeYearSeries(years), true, true);
  window.addEventListener('resize', () => charts.forEach((chart) => chart.resize()));
}

if (typeof document !== 'undefined') initializeCharts();
