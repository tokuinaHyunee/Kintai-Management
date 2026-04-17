import { useState, useEffect } from "react";
import { adminApi } from "../../api/api";
import type { EmployeeMonthlySummary } from "../../types";
import "./AdminMonthlyRecord.css";

export default function AdminMonthlyRecord() {
  const today = new Date();
  const [month,   setMonth]   = useState(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`);
  const [records, setRecords] = useState<EmployeeMonthlySummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState("");

  useEffect(() => { fetchData(); }, [month]);

  const fetchData = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await adminApi.getMonthlySummary({ month });
      setRecords(res.data ?? []);
    } catch {
      setError("データの読み込みに失敗しました");
      setRecords([]);
    } finally {
      setLoading(false);
    }
  };

  const [y, m] = month.split("-");
  const totalWorkDays = records.reduce((s, r) => s + r.workDays, 0);
  const totalHours    = records.reduce((s, r) => s + r.totalHours, 0).toFixed(1);
  const totalOvertime = records.reduce((s, r) => s + r.overtimeHours, 0).toFixed(1);

  const handlePdfPrint = () => {
    const win = window.open("", "_blank");
    if (!win) return;

    const rows = records.map((r) => `
      <tr>
        <td>${r.loginId}</td>
        <td>${r.employeeName}</td>
        <td>${r.department ?? "—"}</td>
        <td class="num">${r.workDays}</td>
        <td class="num">${r.totalHours}</td>
        <td class="num ${r.overtimeHours > 0 ? "overtime" : ""}">${r.overtimeHours}</td>
      </tr>
    `).join("");

    win.document.write(`
      <!DOCTYPE html>
      <html lang="ja">
      <head>
        <meta charset="UTF-8" />
        <title>${y}年${m}月 全社員勤怠実績</title>
        <style>
          * { box-sizing: border-box; margin: 0; padding: 0; }
          body { font-family: "Meiryo", "Hiragino Kaku Gothic Pro", sans-serif; font-size: 11px; color: #111; padding: 20px; }
          h1 { font-size: 16px; font-weight: 700; margin-bottom: 4px; }
          .sub { font-size: 11px; color: #555; margin-bottom: 16px; }
          table { width: 100%; border-collapse: collapse; }
          th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }
          th { background: #f0f4f8; font-weight: 600; }
          .num { text-align: right; }
          .overtime { color: #dc2626; font-weight: 600; }
          .summary { margin-bottom: 14px; display: flex; gap: 24px; }
          .summary-item { }
          .summary-label { font-size: 10px; color: #555; }
          .summary-value { font-size: 14px; font-weight: 700; }
          @media print {
            body { padding: 0; }
            @page { margin: 15mm; }
          }
        </style>
      </head>
      <body>
        <h1>${y}年${m}月 全社員勤怠実績</h1>
        <div class="sub">印刷日: ${new Date().toLocaleDateString("ja-JP")}</div>
        <div class="summary">
          <div class="summary-item">
            <div class="summary-label">対象社員数</div>
            <div class="summary-value">${records.length}名</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">総出勤日数</div>
            <div class="summary-value">${totalWorkDays}日</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">総労働時間</div>
            <div class="summary-value">${totalHours}h</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">総残業時間</div>
            <div class="summary-value">${totalOvertime}h</div>
          </div>
        </div>
        <table>
          <thead>
            <tr>
              <th>社員番号</th><th>氏名</th><th>部署</th>
              <th>出勤日数</th><th>総労働(h)</th><th>残業(h)</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
        <script>window.onload = function() { window.print(); };</script>
      </body>
      </html>
    `);
    win.document.close();
  };

  return (
    <div className="admin-monthly">
      <div className="breadcrumb">ホーム <span className="bc-sep">›</span> <span>全社員月次実績</span></div>

      <div className="page-header">
        <div>
          <div className="page-title">全社員月次勤怠実績</div>
          <div className="page-subtitle">{y}年{m}月 全社員集計</div>
        </div>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <input type="month" className="input month-input" value={month} onChange={(e) => setMonth(e.target.value)} />
          <button className="btn btn-secondary" onClick={handlePdfPrint} disabled={records.length === 0}>
            PDF出力
          </button>
        </div>
      </div>

      <div className="summary-hero">
        <div className="summary-hero-item">
          <div className="summary-hero-label">対象社員数</div>
          <div className="summary-hero-value">{records.length}<span className="hero-unit">名</span></div>
        </div>
        <div className="summary-hero-divider" />
        <div className="summary-hero-item">
          <div className="summary-hero-label">総出勤日数</div>
          <div className="summary-hero-value">{totalWorkDays}<span className="hero-unit">日</span></div>
        </div>
        <div className="summary-hero-divider" />
        <div className="summary-hero-item">
          <div className="summary-hero-label">総労働時間</div>
          <div className="summary-hero-value">{totalHours}<span className="hero-unit">h</span></div>
        </div>
        <div className="summary-hero-divider" />
        <div className="summary-hero-item">
          <div className="summary-hero-label">総残業時間</div>
          <div className="summary-hero-value overtime">{totalOvertime}<span className="hero-unit">h</span></div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "14px" }}>
        <div className="card-title">社員別集計 ({m}月)</div>
        {loading ? (
          <div className="no-data">読み込み中...</div>
        ) : error ? (
          <div className="no-data" style={{ color: "var(--red-text)" }}>{error}</div>
        ) : (
          <table className="tbl">
            <thead>
              <tr>
                <th>社員番号</th><th>氏名</th><th>部署</th>
                <th>出勤日数</th><th>総労働(h)</th><th>残業(h)</th><th>状態</th>
              </tr>
            </thead>
            <tbody>
              {records.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", color: "var(--text4)", padding: "20px" }}>
                    データがありません
                  </td>
                </tr>
              ) : records.map((r, i) => (
                <tr key={i}>
                  <td><span className="code-badge">{r.loginId}</span></td>
                  <td className="name-cell">{r.employeeName}</td>
                  <td>{r.department ?? "—"}</td>
                  <td>{r.workDays}<span style={{ fontSize: "11px", color: "var(--text4)" }}> 日</span></td>
                  <td>{r.totalHours}<span style={{ fontSize: "11px", color: "var(--text4)" }}> h</span></td>
                  <td className={r.overtimeHours > 0 ? "overtime-cell" : ""}>
                    {r.overtimeHours}<span style={{ fontSize: "11px" }}> h</span>
                  </td>
                  <td>
                    {r.workDays > 0
                      ? <span className="badge badge-green">実績あり</span>
                      : <span className="badge badge-gray">未出勤</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
