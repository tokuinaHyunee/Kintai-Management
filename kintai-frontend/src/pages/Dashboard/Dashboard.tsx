import { useState, useEffect } from "react";
import { attendanceApi, summaryApi } from "../../api/api";
import type { User, WorkTimeRecord, MonthlySummary } from "../../types";
import { formatLocalDate, formatLocalMonth } from "../../utils/date";
import "./Dashboard.css";

interface Props { user: User | null; }

function getErrMsg(error: unknown, fallback: string): string {
  const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  if (msg) return msg;
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

export default function Dashboard({ user }: Props) {
  const [currentTime,      setCurrentTime]      = useState(new Date());
  const [todayRecord,      setTodayRecord]      = useState<WorkTimeRecord | null>(null);
  const [summary,          setSummary]          = useState<MonthlySummary | null>(null);
  const [recentList,       setRecentList]       = useState<WorkTimeRecord[]>([]);
  const [memo,             setMemo]             = useState("");
  const [memoSaved,        setMemoSaved]        = useState(false);
  const [loading,          setLoading]          = useState(false);
  const [attendanceLoaded, setAttendanceLoaded] = useState(false);
  const [message,          setMessage]          = useState("");

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => { fetchData(); }, []);

  const fetchData = async () => {
    const today  = new Date();
    const month  = formatLocalMonth(today);
    const todayStr = formatLocalDate(today);

    const [listRes, summaryRes] = await Promise.allSettled([
      attendanceApi.getList({ month }),
      summaryApi.getMonthly({ month }),
    ]);

    if (listRes.status === "fulfilled") {
      const list = listRes.value.data ?? [];
      setRecentList(list);
      const todayRec = list.find((r) => r.workDate === todayStr) ?? null;
      setTodayRecord(todayRec);
      setMemo(todayRec?.workMemo ?? "");
      setAttendanceLoaded(true);
    } else {
      setRecentList([]);
      setTodayRecord(null);
      setAttendanceLoaded(false);
      setMessage(getErrMsg(listRes.reason, "勤怠一覧の読み込みに失敗しました"));
    }

    if (summaryRes.status === "fulfilled") {
      setSummary(summaryRes.value.data ?? null);
    } else {
      setSummary({ workDays: 0, totalHours: 0, overtimeHours: 0, paidLeaveDays: 0 });
    }
  };

  const stamp = async (apiFn: () => Promise<unknown>, successMsg: string, errorMsg: string) => {
    if (!attendanceLoaded) {
      setMessage("勤怠情報の読み込みに失敗しました。再読み込みして再度お試しください。");
      setTimeout(() => setMessage(""), 3000);
      return;
    }
    setLoading(true);
    try {
      await apiFn();
      setMessage(successMsg);
      await fetchData();
    } catch (e: unknown) {
      setMessage(getErrMsg(e, errorMsg));
    } finally {
      setLoading(false);
      setTimeout(() => setMessage(""), 3000);
    }
  };

  const handleClockIn     = () => stamp(attendanceApi.clockIn,     "出勤を記録しました",        "出勤処理に失敗しました");
  const handleClockOut    = () => stamp(attendanceApi.clockOut,    "退勤を記録しました",        "退勤処理に失敗しました");
  const handleGoOut       = () => stamp(attendanceApi.goOut,       "外出を記録しました",        "外出処理に失敗しました");
  const handleGoOutReturn = () => stamp(attendanceApi.goOutReturn, "外出戻りを記録しました",    "外出戻り処理に失敗しました");

  const handleSaveMemo = async () => {
    const today = formatLocalDate(new Date());
    try {
      await attendanceApi.saveMemo({ work_date: today, memo });
      setMemoSaved(true);
      setTimeout(() => setMemoSaved(false), 2000);
      await fetchData();
    } catch (e: unknown) {
      setMessage(getErrMsg(e, "業務内容の保存に失敗しました"));
      setTimeout(() => setMessage(""), 3000);
    }
  };

  const hasClockedIn  = !!todayRecord?.startTime;
  const hasClockedOut = !!todayRecord?.endTime;
  const isOut         = hasClockedIn && !hasClockedOut && !!todayRecord?.outTime && !todayRecord?.returnTime;
  const hasReturned   = !!todayRecord?.returnTime;

  const fmt     = (d: Date) => d.toLocaleTimeString("ja-JP", { hour: "2-digit", minute: "2-digit" });
  const fmtDate = (d: Date) => d.toLocaleDateString("ja-JP", { year: "numeric", month: "long", day: "numeric", weekday: "short" });
  const h       = new Date().getHours();
  const greeting = h < 12 ? "おはようございます" : h < 18 ? "こんにちは" : "お疲れ様です";

  return (
    <div className="dashboard">
      <div className="breadcrumb">ホーム <span className="bc-sep">›</span> <span>出退勤</span></div>
      <div className="page-header">
        <div>
          <div className="page-title">出退勤</div>
          <div className="page-subtitle">{greeting}、{user?.employeeName}さん</div>
        </div>
        <div className="dashboard-date">{fmtDate(currentTime)}</div>
      </div>

      {message && <div className="dashboard-message">{message}</div>}

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-label">今月の出勤日数</div>
          <div className="stat-value">{summary?.workDays ?? "—"}<span className="stat-unit">日</span></div>
        </div>
        <div className="stat-card">
          <div className="stat-label">今月の総労働時間</div>
          <div className="stat-value">{summary?.totalHours ?? "—"}<span className="stat-unit">h</span></div>
        </div>
        <div className="stat-card">
          <div className="stat-label">今月の残業時間</div>
          <div className="stat-value overtime">{summary?.overtimeHours ?? "—"}<span className="stat-unit">h</span></div>
        </div>
        <div className="stat-card">
          <div className="stat-label">有給休暇残日数</div>
          <div className="stat-value blue">{summary?.paidLeaveDays ?? "—"}<span className="stat-unit">日</span></div>
        </div>
      </div>

      <div className="dashboard-row">
        <div className="card">
          <div className="card-title">本日のタイムスタンプ</div>
          <div className="clock-display">
            <div className="clock-time">{fmt(currentTime)}</div>
            <div className="clock-date">{fmtDate(currentTime)} 現在</div>
          </div>
          <div className="stamp-row">
            {hasClockedIn ? (
              <div className="stamp-btn stamp-done">
                出勤済<span className="stamp-sub">{todayRecord?.startTime} 処理済</span>
              </div>
            ) : (
              <button className="stamp-btn stamp-in" onClick={handleClockIn} disabled={loading || !attendanceLoaded}>
                出勤<span className="stamp-sub">ボタンを押して記録</span>
              </button>
            )}
            {hasClockedOut ? (
              <div className="stamp-btn stamp-done">
                退勤済<span className="stamp-sub">{todayRecord?.endTime} 処理済</span>
              </div>
            ) : (
              <button
                className={`stamp-btn stamp-out ${!hasClockedIn ? "disabled" : ""}`}
                onClick={handleClockOut}
                disabled={loading || !attendanceLoaded || !hasClockedIn}
              >
                退勤<span className="stamp-sub">ボタンを押して記録</span>
              </button>
            )}
          </div>
          {hasClockedIn && !hasClockedOut && (
            <div className="stamp-row stamp-row-sub">
              {isOut ? (
                <div className="stamp-btn stamp-out-active">
                  外出中<span className="stamp-sub">{todayRecord?.outTime} 外出</span>
                </div>
              ) : (
                <button
                  className="stamp-btn stamp-go-out"
                  onClick={handleGoOut}
                  disabled={loading || !attendanceLoaded || hasReturned}
                >
                  外出<span className="stamp-sub">外出時に押す</span>
                </button>
              )}
              {hasReturned ? (
                <div className="stamp-btn stamp-done">
                  外出戻り済<span className="stamp-sub">{todayRecord?.returnTime} 戻り</span>
                </div>
              ) : (
                <button
                  className={`stamp-btn stamp-go-return ${!isOut ? "disabled" : ""}`}
                  onClick={handleGoOutReturn}
                  disabled={loading || !attendanceLoaded || !isOut}
                >
                  外出戻り<span className="stamp-sub">戻り時に押す</span>
                </button>
              )}
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-title">本日の業務内容</div>
          <textarea
            className="input memo-input"
            placeholder={"本日の業務内容を入力してください\n例: ○○機能 設計・実装、チームミーティング参加"}
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
          />
          <div className="memo-actions">
            <button className="btn btn-primary btn-sm" onClick={handleSaveMemo}>
              {memoSaved ? "保存済 ✓" : "保存"}
            </button>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "14px" }}>
        <div className="card-title">最近の勤務記録</div>
        <div style={{ maxHeight: "180px", overflowY: "auto" }}>
          <table className="tbl">
            <thead>
              <tr>
                <th>日付</th><th>出勤</th><th>退勤</th><th>外出</th><th>外出戻り</th>
                <th>労働時間</th><th>残業</th><th>業務内容</th><th>状態</th>
              </tr>
            </thead>
            <tbody>
              {recentList.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: "center", color: "var(--text4)", padding: "20px" }}>
                    勤務記録がありません
                  </td>
                </tr>
              ) : recentList.map((r, i) => (
                <tr key={i}>
                  <td>{r.workDate}</td>
                  <td>{r.startTime ?? "—"}</td>
                  <td>{r.endTime ?? "—"}</td>
                  <td>{r.outTime ?? "—"}</td>
                  <td>{r.returnTime ?? "—"}</td>
                  <td>{r.workMinutes ? `${Math.floor(r.workMinutes / 60)}h${r.workMinutes % 60}m` : "勤務中"}</td>
                  <td>{r.overtimeMinutes ? `${Math.floor(r.overtimeMinutes / 60)}h${r.overtimeMinutes % 60}m` : "—"}</td>
                  <td>{r.workMemo ?? "—"}</td>
                  <td>{r.endTime ? <span className="badge badge-green">確定</span> : <span className="badge badge-blue">記録中</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
