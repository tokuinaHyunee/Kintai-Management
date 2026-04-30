import { useState, useEffect } from "react";
import { summaryApi, attendanceApi } from "../../api/api";
import type { WorkTimeRecord, MonthlySummary, WeekSummary } from "../../types";
import { formatLocalDate, formatLocalMonth } from "../../utils/date";
import "./MonthlyRecord.css";

const fmtMin = (min: number | null): string => {
  if (!min) return "—";
  return `${Math.floor(min / 60)}h${String(min % 60).padStart(2, "0")}m`;
};
const fmtHours = (h: number | null | undefined): string => {
  if (h == null) return "—";
  const hrs = Math.floor(h);
  const min = Math.round((h - hrs) * 60);
  return `${hrs}h ${String(min).padStart(2, "0")}m`;
};

export default function MonthlyRecord() {
  const today = new Date();
  const [month,      setMonth]      = useState(formatLocalMonth(today));
  const [viewMode,   setViewMode]   = useState<"monthly" | "weekly">("monthly");
  const [weekOffset, setWeekOffset] = useState(0);
  const [summary,    setSummary]    = useState<MonthlySummary | null>(null);
  const [records,    setRecords]    = useState<WorkTimeRecord[]>([]);
  const [loading,    setLoading]    = useState(false);
  const [holidays,   setHolidays]   = useState<Record<string, string>>({});

  useEffect(() => { fetchData(); setWeekOffset(0); }, [month]);

  const year = month.split("-")[0];

  useEffect(() => {
    fetch(`https://holidays-jp.github.io/api/v1/${year}/date.json`)
      .then((r) => r.json())
      .then((data: Record<string, string>) => setHolidays(data))
      .catch(() => setHolidays({}));
  }, [year]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [summaryRes, listRes] = await Promise.all([
        summaryApi.getMonthly({ month }),
        attendanceApi.getList({ month }),
      ]);
      setSummary(summaryRes.data ?? null);
      setRecords(listRes.data ?? []);
    } catch {
      setSummary(null);
      setRecords([]);
    } finally {
      setLoading(false);
    }
  };

  const buildCalendar = (): (number | null)[] => {
    const [y, m] = month.split("-").map(Number);
    const firstDay = new Date(y, m - 1, 1).getDay();
    const lastDate = new Date(y, m, 0).getDate();
    const cells: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) cells.push(null);
    for (let d = 1; d <= lastDate; d++) cells.push(d);
    return cells;
  };

  const getDateStr = (d: number) => {
    const [y, m] = month.split("-");
    return `${y}-${m}-${String(d).padStart(2, "0")}`;
  };
  const getRecordForDate = (d: number | null) => d ? records.find((r) => r.workDate === getDateStr(d)) : null;
  const isToday   = (d: number | null) => !!d && getDateStr(d) === formatLocalDate(today);
  const isWeekend = (idx: number) => idx % 7 === 0 || idx % 7 === 6;

  const getWeekRange = () => {
    const [y, mo] = month.split("-").map(Number);
    const currentMonth = formatLocalMonth(today);
    const base = month === currentMonth
      ? new Date(today.getFullYear(), today.getMonth(), today.getDate())
      : new Date(y, mo, 0);
    const dow = base.getDay();
    const diffToMon = dow === 0 ? -6 : 1 - dow;
    const start = new Date(base);
    start.setDate(base.getDate() + diffToMon + weekOffset * 7);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    return { startStr: fmt(start), endStr: fmt(end) };
  };

  const buildWeekSummaries = (): WeekSummary[] => {
    const weeks: WeekSummary[] = [];
    let weekStart: string | null = null;
    let weekRecords: WorkTimeRecord[] = [];
    records.forEach((r) => {
      const dow = new Date(r.workDate).getDay();
      if (dow === 1 || weekStart === null) {
        if (weekStart !== null) weeks.push({ start: weekStart, records: weekRecords });
        weekStart = r.workDate; weekRecords = [];
      }
      weekRecords.push(r);
    });
    if (weekStart) weeks.push({ start: weekStart, records: weekRecords });
    return weeks;
  };

  const weekRange       = viewMode === "weekly" ? getWeekRange() : null;
  const filteredRecords = weekRange
    ? records.filter((r) => r.workDate >= weekRange.startStr && r.workDate <= weekRange.endStr)
    : records;
  const calendar      = buildCalendar();
  const weekSummaries = buildWeekSummaries();
  const [y, m]        = month.split("-");

  return (
    <div className="monthly">
      <div className="breadcrumb">ホーム <span className="bc-sep">›</span> <span>月次集計</span></div>
      <div className="page-header">
        <div>
          <div className="page-title">月次勤怠実績</div>
          <div className="page-subtitle">{y}年{m}月 集計</div>
        </div>
        <div className="monthly-controls">
          <input type="month" className="input month-input" value={month} onChange={(e) => setMonth(e.target.value)} />
          <button className={`btn ${viewMode === "weekly" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setViewMode("weekly")}>週間</button>
          <button className={`btn ${viewMode === "monthly" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setViewMode("monthly")}>月間</button>
        </div>
      </div>

      <div className="summary-hero">
        <div className="summary-hero-item">
          <div className="summary-hero-label">総労働時間</div>
          <div className="summary-hero-value">{fmtHours(summary?.totalHours)}</div>
        </div>
        <div className="summary-hero-divider" />
        <div className="summary-hero-item">
          <div className="summary-hero-label">残業時間</div>
          <div className="summary-hero-value overtime">{fmtHours(summary?.overtimeHours)}</div>
        </div>
        <div className="summary-hero-divider" />
        <div className="summary-hero-item">
          <div className="summary-hero-label">出勤日数</div>
          <div className="summary-hero-value">{summary?.workDays ?? "—"}<span className="hero-unit">日</span></div>
        </div>
        <div className="summary-hero-divider" />
        <div className="summary-hero-item">
          <div className="summary-hero-label">有給休暇取得</div>
          <div className="summary-hero-value">{summary?.paidLeaveDays ?? "0"}<span className="hero-unit">日</span></div>
        </div>
      </div>

      <div className="monthly-grid">
        <div className="card">
          <div className="card-title">週間集計</div>
          <div className="week-list">
            {weekSummaries.length === 0 ? (
              <div className="no-data">データなし</div>
            ) : weekSummaries.map((w, i) => {
              const totalMin = w.records.reduce((s, r) => s + (r.workMinutes ?? 0), 0);
              const pct = Math.min((totalMin / (40 * 60)) * 100, 100);
              return (
                <div key={i} className="week-item">
                  <div className="week-header">
                    <span className="week-label">第{i + 1}週 ({w.start}〜)</span>
                    <span className={`week-total ${totalMin > 0 ? "active" : ""}`}>
                      {totalMin > 0 ? fmtMin(totalMin) : "未確定"}
                    </span>
                  </div>
                  <div className="week-bar"><div className="week-bar-fill" style={{ width: `${pct}%` }} /></div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="card">
          <div className="card-title">{m}月 カレンダー</div>
          <div className="cal-grid">
            {["日","月","火","水","木","金","土"].map((d, i) => (
              <div key={d} className={`cal-head ${i === 0 ? "sun" : i === 6 ? "sat" : ""}`}>{d}</div>
            ))}
            {calendar.map((d, idx) => {
              const rec         = getRecordForDate(d);
              const dateStr     = d ? getDateStr(d) : "";
              const holidayName = d ? (holidays[dateStr] ?? null) : null;
              const isHoliday   = !!holidayName;
              const isWork      = !!(rec && rec.startTime);
              const leaveType   = rec?.leaveType ?? null;
              const isAnnual    = leaveType === "ANNUAL";
              const isHalf      = leaveType === "HALF_MORNING" || leaveType === "HALF_AFTERNOON";
              const isSick      = leaveType === "SICK";
              const isAbsent    = leaveType === "ABSENT";
              const leaveLabel  = isAnnual ? "有給" : isHalf ? "半休" : isSick ? "病休" : null;
              const leaveCls    = isAnnual ? "annual" : isHalf ? "half" : isSick ? "sick" : isAbsent ? "absent" : "";
              const holidayCls  = isHoliday && !leaveCls && !isWork ? "holiday" : "";
              return (
                <div
                  key={idx}
                  className={`cal-cell ${!d ? "empty" : ""} ${isToday(d) ? "today" : ""} ${isWork && !isToday(d) && !leaveCls && !isHoliday ? "work" : ""} ${leaveCls} ${holidayCls} ${isWeekend(idx) && d ? "weekend" : ""}`}
                  title={holidayName ?? undefined}
                >
                  {d && (
                    isToday(d)
                      ? <span className="today-dot">{d}</span>
                      : <span className="cal-day">
                          {d}
                          {leaveLabel && <span className="leave-label">{leaveLabel}</span>}
                          {isHoliday && !leaveLabel && <span className="leave-label holiday-label">{holidayName!.length > 4 ? holidayName!.slice(0, 3) + "…" : holidayName}</span>}
                        </span>
                  )}
                </div>
              );
            })}
          </div>
          <div className="cal-legend">
            <span><span className="legend-dot work"    />出勤</span>
            <span><span className="legend-dot today"   />今日</span>
            <span><span className="legend-dot holiday" />祝日</span>
            <span><span className="legend-dot annual"  />有給</span>
            <span><span className="legend-dot half"    />半休</span>
            <span><span className="legend-dot sick"    />病休</span>
            <span><span className="legend-dot absent"  />欠勤</span>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "14px" }}>
        <div className="card-title" style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          {viewMode === "weekly" && weekRange ? (
            <>
              <span>週間勤務記録 ({weekRange.startStr} 〜 {weekRange.endStr})</span>
              <div style={{ display: "flex", gap: "6px", marginLeft: "auto" }}>
                <button className="btn btn-secondary btn-sm" onClick={() => setWeekOffset((v) => v - 1)}>◀ 前週</button>
                <button className="btn btn-secondary btn-sm" onClick={() => setWeekOffset(0)}>今週</button>
                <button className="btn btn-secondary btn-sm" onClick={() => setWeekOffset((v) => v + 1)}>次週 ▶</button>
              </div>
            </>
          ) : <span>勤務記録一覧 ({m}月)</span>}
        </div>
        {loading ? <div className="no-data">読み込み中...</div> : (
          <table className="tbl">
            <thead>
              <tr>
                <th>日付</th><th>出勤</th><th>退勤</th>
                <th>労働時間</th><th>残業</th><th>業務内容</th><th>状態</th>
              </tr>
            </thead>
            <tbody>
              {filteredRecords.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", color: "var(--text4)", padding: "20px" }}>
                    勤務記録がありません
                  </td>
                </tr>
              ) : filteredRecords.map((r, i) => (
                <tr key={i}>
                  <td>{r.workDate}</td>
                  <td>{r.startTime ?? "—"}</td>
                  <td>{r.endTime ?? "—"}</td>
                  <td>{fmtMin(r.workMinutes)}</td>
                  <td className={r.overtimeMinutes && r.overtimeMinutes > 0 ? "overtime-cell" : ""}>{fmtMin(r.overtimeMinutes)}</td>
                  <td className="memo-cell">{r.workMemo ?? "—"}</td>
                  <td>{r.endTime ? <span className="badge badge-green">確定</span> : <span className="badge badge-blue">記録中</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
