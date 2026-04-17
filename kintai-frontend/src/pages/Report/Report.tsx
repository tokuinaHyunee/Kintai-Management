import { useState } from "react";
import { summaryApi, adminApi } from "../../api/api";
import type { User } from "../../types";
import "./Report.css";

interface Props {
  user: User | null;
}

export default function Report({ user }: Props) {
  const isAdmin = user?.role === "ADMIN";
  const today   = new Date();
  const [month,      setMonth]      = useState(
    `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`
  );
  const [csvLoading, setCsvLoading] = useState(false);
  const [message,    setMessage]    = useState({ text: "", type: "" });

  const showMsg = (text: string, type: string) => {
    setMessage({ text, type });
    setTimeout(() => setMessage({ text: "", type: "" }), 4000);
  };

  const handleCsvExport = async () => {
    setCsvLoading(true);
    try {
      if (isAdmin) {
        const res     = await adminApi.getMonthlySummary({ month });
        const records = res.data ?? [];
        const header  = "社員コード,社員名,部署,出勤日数,総労働時間(h),残業時間(h)\n";
        const rows    = records.map(
          (r) => `${r.employeeCode},${r.employeeName},${r.department ?? ""},${r.workDays},${r.totalHours},${r.overtimeHours}`
        ).join("\n");
        const blob = new Blob(["\uFEFF" + header + rows], { type: "text/csv;charset=utf-8;" });
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement("a");
        a.href = url; a.download = `admin_kintai_${month}.csv`; a.click();
        URL.revokeObjectURL(url);
      } else {
        const res = await summaryApi.exportCsv(month);
        const url = URL.createObjectURL(res.data);
        const a   = document.createElement("a");
        a.href = url; a.download = `kintai_${month}.csv`; a.click();
        URL.revokeObjectURL(url);
      }
      showMsg("CSVファイルをダウンロードしました", "success");
    } catch {
      showMsg("CSV出力に失敗しました", "error");
    } finally {
      setCsvLoading(false);
    }
  };

  const [y, m] = month.split("-");

  return (
    <div className="report-page">
      <div className="breadcrumb">
        ホーム <span className="bc-sep">›</span> <span>帳票出力</span>
      </div>

      <div className="page-header">
        <div>
          <div className="page-title">帳票出力</div>
          <div className="page-subtitle">{y}年{m}月 勤怠データの出力</div>
        </div>
        <input
          type="month"
          className="input month-input"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
        />
      </div>

      {message.text && (
        <div className={`report-message ${message.type}`}>{message.text}</div>
      )}

      <div className="card report-card">
        <div className="report-card-icon csv-icon">CSV</div>
        <div className="report-card-title">CSV出力</div>
        <div className="report-card-desc">
          {isAdmin
            ? `${y}年${m}月の全社員勤怠集計をCSV形式でダウンロードします`
            : `${y}年${m}月の自分の勤怠実績をCSV形式でダウンロードします`}
        </div>
        <div className="report-card-meta">
          <span className="report-tag">形式：CSV（UTF-8 BOM付き）</span>
          <span className="report-tag">対象月：{y}年{m}月</span>
        </div>
        <button
          className="btn btn-primary report-btn"
          onClick={handleCsvExport}
          disabled={csvLoading}
        >
          {csvLoading ? "出力中..." : "CSV出力"}
        </button>
      </div>
    </div>
  );
}
