import { useState, useEffect } from "react";
import { attendanceApi, summaryApi, leaveApi } from "../../api/api";
import type { User, WorkTimeRecord, MonthlySummary, LeaveRequest, LeaveType } from "../../types";
import { formatLocalDate, formatLocalMonth } from "../../utils/date";
import { getErrMsg } from "../../utils/error";
import { LEAVE_TYPE_OPTIONS, STATUS_BADGE } from "../../utils/labels";
import "./Dashboard.css";

interface Props { user: User | null; }

export default function Dashboard({ user }: Props) {
  const [tab,              setTab]              = useState<"stamp" | "leave">("stamp");
  const [currentTime,      setCurrentTime]      = useState(new Date());
  const [todayRecord,      setTodayRecord]      = useState<WorkTimeRecord | null>(null);
  const [summary,          setSummary]          = useState<MonthlySummary | null>(null);
  const [recentList,       setRecentList]       = useState<WorkTimeRecord[]>([]);
  const [memo,             setMemo]             = useState("");
  const [memoSaved,        setMemoSaved]        = useState(false);
  const [loading,          setLoading]          = useState(false);
  const [attendanceLoaded, setAttendanceLoaded] = useState(false);
  const [message,          setMessage]          = useState("");

  // 休暇申請
  const [leaveList,    setLeaveList]    = useState<LeaveRequest[]>([]);
  const [leaveForm,    setLeaveForm]    = useState({ leaveType: "ANNUAL" as LeaveType, leaveDate: formatLocalDate(new Date()), reason: "" });
  const [leaveMsg,     setLeaveMsg]     = useState({ text: "", isError: false });
  const [leaveLoading, setLeaveLoading] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => { fetchData(); fetchLeaves(); }, []);

  const fetchData = async () => {
    const today    = new Date();
    const month    = formatLocalMonth(today);
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

  const fetchLeaves = async () => {
    try {
      const res = await leaveApi.getMyLeaves();
      setLeaveList(res.data ?? []);
    } catch {
      setLeaveList([]);
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

  const handleClockIn     = () => stamp(attendanceApi.clockIn,     "出勤を記録しました",     "出勤処理に失敗しました");
  const handleClockOut    = () => stamp(attendanceApi.clockOut,    "退勤を記録しました",     "退勤処理に失敗しました");
  const handleGoOut       = () => stamp(attendanceApi.goOut,       "外出を記録しました",     "外出処理に失敗しました");
  const handleGoOutReturn = () => stamp(attendanceApi.goOutReturn, "外出戻りを記録しました", "外出戻り処理に失敗しました");

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

  const handleLeaveSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLeaveLoading(true);
    setLeaveMsg({ text: "", isError: false });
    try {
      await leaveApi.apply(leaveForm);
      setLeaveMsg({ text: "休暇申請を送信しました", isError: false });
      setLeaveForm({ leaveType: "ANNUAL", leaveDate: formatLocalDate(new Date()), reason: "" });
      await fetchLeaves();
    } catch (e: unknown) {
      setLeaveMsg({ text: getErrMsg(e, "申請に失敗しました"), isError: true });
    } finally {
      setLeaveLoading(false);
      setTimeout(() => setLeaveMsg({ text: "", isError: false }), 4000);
    }
  };

  const hasClockedIn  = !!todayRecord?.startTime;
  const hasClockedOut = !!todayRecord?.endTime;
  const lastGoOut     = todayRecord?.goOutRecords?.at(-1);
  const isOut         = hasClockedIn && !hasClockedOut && !!lastGoOut && !lastGoOut.returnTime;

  const fmt     = (d: Date) => d.toLocaleTimeString("ja-JP", { hour: "2-digit", minute: "2-digit" });
  const fmtDate = (d: Date) => d.toLocaleDateString("ja-JP", { year: "numeric", month: "long", day: "numeric", weekday: "short" });
  const h = new Date().getHours();
  const greeting = h < 12 ? "おはようございます" : h < 18 ? "こんにちは" : "お疲れ様です";

  return (
    <div className="dashboard">
      <div className="breadcrumb">ホーム <span className="bc-sep">›</span> <span>{tab === "stamp" ? "出退勤" : "休暇申請"}</span></div>
      <div className="page-header">
        <div>
          <div className="page-title">{tab === "stamp" ? "出退勤" : "休暇申請"}</div>
          <div className="page-subtitle">{greeting}、{user?.employeeName}さん</div>
        </div>
        <div className="dashboard-date">{fmtDate(currentTime)}</div>
      </div>

      <div className="dash-tabs">
        <button className={`dash-tab ${tab === "stamp" ? "active" : ""}`} onClick={() => setTab("stamp")}>出退勤</button>
        <button className={`dash-tab ${tab === "leave" ? "active" : ""}`} onClick={() => setTab("leave")}>休暇申請</button>
      </div>

      {/* ===== 出退勤タブ ===== */}
      {tab === "stamp" && (
        <>
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
                      外出中<span className="stamp-sub">{lastGoOut?.outTime} 外出</span>
                    </div>
                  ) : (
                    <button
                      className="stamp-btn stamp-go-out"
                      onClick={handleGoOut}
                      disabled={loading || !attendanceLoaded}
                    >
                      外出<span className="stamp-sub">外出時に押す</span>
                    </button>
                  )}
                  <button
                    className={`stamp-btn stamp-go-return ${!isOut ? "disabled" : ""}`}
                    onClick={handleGoOutReturn}
                    disabled={loading || !attendanceLoaded || !isOut}
                  >
                    外出戻り<span className="stamp-sub">戻り時に押す</span>
                  </button>
                </div>
              )}
              {hasClockedIn && (todayRecord?.goOutRecords?.length ?? 0) > 0 && (
                <div className="go-out-history">
                  {todayRecord!.goOutRecords!.map((g, i) => (
                    <div key={i} className="go-out-history-item">
                      外出{i + 1}: {g.outTime} 〜 {g.returnTime ?? "外出中"}
                    </div>
                  ))}
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
                      <td>
                        {(r.goOutRecords?.length ?? 0) > 0
                          ? r.goOutRecords!.map((g, j) => <div key={j}>{g.outTime}</div>)
                          : "—"}
                      </td>
                      <td>
                        {(r.goOutRecords?.length ?? 0) > 0
                          ? r.goOutRecords!.map((g, j) => <div key={j}>{g.returnTime ?? "外出中"}</div>)
                          : "—"}
                      </td>
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
        </>
      )}

      {/* ===== 休暇申請タブ ===== */}
      {tab === "leave" && (
        <>
          <div className="card" style={{ marginTop: "14px" }}>
            <div className="card-title">休暇申請</div>
            <form className="leave-form" onSubmit={handleLeaveSubmit}>
              <div className="leave-form-row">
                <div className="leave-form-field">
                  <label className="form-label">休暇種別 <span className="required">*</span></label>
                  <select
                    className="input"
                    value={leaveForm.leaveType}
                    onChange={(e) => setLeaveForm({ ...leaveForm, leaveType: e.target.value as LeaveType })}
                  >
                    {LEAVE_TYPE_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
                <div className="leave-form-field">
                  <label className="form-label">休暇取得日 <span className="required">*</span></label>
                  <input
                    type="date"
                    className="input"
                    value={leaveForm.leaveDate}
                    onChange={(e) => setLeaveForm({ ...leaveForm, leaveDate: e.target.value })}
                    required
                  />
                </div>
              </div>
              <div className="leave-form-field">
                <label className="form-label">申請理由</label>
                <textarea
                  className="input leave-reason-input"
                  placeholder="申請理由を入力してください（任意）"
                  value={leaveForm.reason}
                  onChange={(e) => setLeaveForm({ ...leaveForm, reason: e.target.value })}
                  maxLength={500}
                />
              </div>
              {leaveMsg.text && (
                <div className={`leave-msg ${leaveMsg.isError ? "leave-msg-error" : "leave-msg-success"}`}>
                  {leaveMsg.text}
                </div>
              )}
              <div className="leave-form-actions">
                <button type="submit" className="btn btn-primary" disabled={leaveLoading}>
                  {leaveLoading ? "送信中..." : "申請する"}
                </button>
              </div>
            </form>
          </div>

          <div className="card" style={{ marginTop: "14px" }}>
            <div className="card-title">申請履歴</div>
            {leaveList.length === 0 ? (
              <div style={{ color: "var(--text4)", padding: "20px", textAlign: "center" }}>
                休暇申請履歴はありません
              </div>
            ) : (
              <table className="tbl">
                <thead>
                  <tr>
                    <th>申請日</th><th>休暇種別</th><th>取得日</th><th>申請理由</th>
                    <th>ステータス</th><th>審査日</th><th>却下理由</th>
                  </tr>
                </thead>
                <tbody>
                  {leaveList.map((r) => (
                    <tr key={r.id}>
                      <td>{r.createdAt}</td>
                      <td>{r.leaveTypeName}</td>
                      <td>{r.leaveDate}</td>
                      <td className="memo-cell">{r.reason ?? "—"}</td>
                      <td>
                        <span className={`badge ${STATUS_BADGE[r.status] ?? "badge-gray"}`}>
                          {r.statusName}
                        </span>
                      </td>
                      <td>{r.reviewedAt ?? "—"}</td>
                      <td className="memo-cell">{r.rejectReason ?? "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
