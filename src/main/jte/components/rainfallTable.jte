@import java.util.List
@import com.pembana.raingauge.dashboard.DashboardResponse

@param List<DashboardResponse.DailyRainfall> days

<div class="table-responsive">
  <table class="table table-vcenter card-table" id="daily-rainfall-table">
    <caption class="visually-hidden">Daily rainfall totals and quality status</caption>
    <thead><tr><th>Hawaiʻi date</th><th>Rainfall</th><th>Status</th></tr></thead>
    <tbody>
      @for (DashboardResponse.DailyRainfall day : days)
        <tr>
          <td>${day.date().toString()}</td>
          <td>${day.display()}</td>
          <td>@template.components.qualityBadge(status = day.status())</td>
        </tr>
      @endfor
    </tbody>
  </table>
</div>
