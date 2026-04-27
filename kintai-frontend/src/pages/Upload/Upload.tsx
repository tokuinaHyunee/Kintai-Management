import { useState, useRef, useEffect } from "react";
import { useLocation } from "react-router-dom";
import { adminApi, csvMailboxApi } from "../../api/api";
import type { User, CsvPreview, ImportResult } from "../../types";
import "./Upload.css";

// 管理者用CSVカラムマッピング
const COLUMN_MAP: Record<string, string> = {
  // 氏名 (列に含まれる場合)
  "社員名": "employeeName", "氏名": "employeeName", "名前": "employeeName",
  "employee_name": "employeeName", "employeename": "employeeName",
  // 日付
  "月日ツキヒ": "workDate", "月日": "workDate", "勤務日": "workDate", "日付": "workDate",
  "work_date": "workDate", "workdate": "workDate",
  // 始業
  "始業時刻シギョウジコク": "startTime", "始業時刻": "startTime", "出勤": "startTime",
  "出勤時刻": "startTime", "start_time": "startTime", "starttime": "startTime",
  // 終業
  "終業時刻シュウギョウジコク": "endTime", "終業時刻": "endTime", "退勤": "endTime",
  "退勤時刻": "endTime", "end_time": "endTime", "endtime": "endTime",
  // 休憩
  "休憩キュウケイ": "breakMinutes", "休憩(分)": "breakMinutes", "休憩": "breakMinutes",
  "break_minutes": "breakMinutes",
  // メモ
  "備考": "workMemo", "業務内容": "workMemo", "備考ビコウ": "workMemo",
  "メモ": "workMemo", "work_memo": "workMemo", "納入物等": "workMemo",
};

const FIELD_LABELS: Record<string, string> = {
  employeeName: "氏名", workDate: "勤務日", startTime: "出勤",
  endTime: "退勤", breakMinutes: "休憩(分)", workMemo: "業務内容",
};

const REQUIRED_FIELDS = ["workDate", "startTime"];

// ヘッダー正規化 (改行・括弧注記除去)
function normalizeHeader(h: string): string {
  return h.replace(/\r?\n[\s\S]*/g, "").replace(/（[^）]*）$/g, "").replace(/\([^)]*\)$/g, "").trim();
}

// "1:00" → "60" (分) 変換
function normalizeBreak(val: string): string {
  if (!val?.trim()) return val;
  const m = val.trim().match(/^(\d+):(\d{2})(?::\d{2})?$/);
  if (m) return String(parseInt(m[1], 10) * 60 + parseInt(m[2], 10));
  return val;
}

function parseCsv(text: string): string[][] {
  return text.replace(/\r\n/g, "\n").replace(/\r/g, "\n").split("\n")
    .filter((l) => l.trim() !== "")
    .map((line) => {
      const cols: string[] = [];
      let cur = "", inQ = false;
      for (let i = 0; i < line.length; i++) {
        const c = line[i];
        if (c === '"') {
          if (inQ && line[i + 1] === '"') { cur += '"'; i++; } else inQ = !inQ;
        } else if (c === "," && !inQ) { cols.push(cur.trim()); cur = ""; }
        else cur += c;
      }
      cols.push(cur.trim());
      return cols;
    });
}

// 「氏名：」ヘッダー行から氏名を抽出
function extractEmployeeName(rows: string[][]): string | null {
  for (const row of rows.slice(0, 30)) {
    for (let i = 0; i < row.length; i++) {
      const raw = row[i].trim();
      const normalized = raw.replace(/[\s\u3000]/g, "").replace(/[：:]/g, "");
      if (normalized === "氏名" || normalized === "氏名欄") {
        for (let j = i + 1; j < row.length; j++) {
          const name = row[j].trim();
          if (name) return name.replace(/\s*[(（][^)）]*[)）]\s*$/, "").trim() || name;
        }
      }
      if (normalized.startsWith("氏名") && normalized.length > 2) {
        const namePart = raw.replace(/^[氏名\s\u3000：:]+/, "").trim();
        if (namePart) return namePart.replace(/\s*[(（][^)）]*[)）]\s*$/, "").trim() || namePart;
      }
    }
  }
  return null;
}

interface Props { user: User | null; }

interface MailboxState {
  csvContent: string;
  fileName: string;
  submissionId: number;
  employeeName?: string;
}

export default function Upload(_: Props) {
  const location = useLocation();
  const today    = new Date();
  const [month,         setMonth]         = useState(
    `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`
  );
  const [csvPreview,    setCsvPreview]    = useState<CsvPreview | null>(null);
  const [extractedName, setExtractedName] = useState<string | null>(null);
  const [importing,     setImporting]     = useState(false);
  const [importResult,  setImportResult]  = useState<(ImportResult & { type: string }) | null>(null);
  const [submissionId,  setSubmissionId]  = useState<number | null>(null);
  const [fromMailbox,   setFromMailbox]   = useState(false);
  // 氏名不一致モーダル
  const [nameErrorMsg,  setNameErrorMsg]  = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // "4月1日(水)" → "YYYY-MM-DD" 変換 (月選択の年を使用)
  const normalizeDate = (val: string): string => {
    if (!val?.trim()) return val;
    const m = val.trim().match(/^(\d+)月(\d+)日(?:\([日月火水木金土]\))?$/);
    if (m) {
      const [y] = month.split("-");
      return `${y}-${String(m[1]).padStart(2, "0")}-${String(m[2]).padStart(2, "0")}`;
    }
    return val;
  };

  // CSV テキストをパースしてプレビューにセット
  const processCsvText = (text: string, fileName: string) => {
    const rows = parseCsv(text);
    if (rows.length < 2) {
      setCsvPreview({ error: "データ行がありません" } as CsvPreview);
      return;
    }

    const empName = extractEmployeeName(rows);
    setExtractedName(empName);

    let headerIdx = 0;
    for (let i = 0; i < Math.min(rows.length, 30); i++) {
      if (rows[i].some((h) => {
        const n = normalizeHeader(h);
        return COLUMN_MAP[n] != null || COLUMN_MAP[n.toLowerCase()] != null;
      })) { headerIdx = i; break; }
    }

    const rawHeaders    = rows[headerIdx];
    const mappedHeaders = rawHeaders.map((h) => {
      const n = normalizeHeader(h);
      return COLUMN_MAP[n] ?? COLUMN_MAP[n.toLowerCase()] ?? null;
    });
    const unknownHeaders = rawHeaders.filter((h, i) => mappedHeaders[i] === null && h.trim() !== "");

    const mappedSet = new Set(mappedHeaders.filter(Boolean));
    const missingRequired = REQUIRED_FIELDS.filter((f) => !mappedSet.has(f));
    const hasEmployeeName = mappedSet.has("employeeName") || !!empName;
    if (!hasEmployeeName) missingRequired.push("employeeName");

    const dataRows = rows.slice(headerIdx + 1)
      .filter((cols) => cols.some((c) => c.trim() !== ""))
      .map((cols) => {
        const obj: Record<string, string> = {};
        rawHeaders.forEach((_, i) => { const key = mappedHeaders[i]; if (key) obj[key] = cols[i] ?? ""; });
        if (obj["workDate"])     obj["workDate"]     = normalizeDate(obj["workDate"]);
        if (obj["breakMinutes"]) obj["breakMinutes"] = normalizeBreak(obj["breakMinutes"]);
        if (!obj["employeeName"] && empName) obj["employeeName"] = empName;
        return obj;
      })
      .filter((obj) => obj["workDate"]?.trim() && obj["startTime"]?.trim());

    setCsvPreview({ rawHeaders, mappedHeaders, unknownHeaders, missingRequired, dataRows, fileName });
  };

  // メールボックスから遷移してきた場合、CSVを自動ロード
  useEffect(() => {
    const state = location.state as MailboxState | null;
    if (state?.csvContent) {
      setFromMailbox(true);
      setSubmissionId(state.submissionId ?? null);
      processCsvText(state.csvContent, state.fileName ?? "受信CSV.csv");
      // ナビゲーション履歴からstateをクリア
      window.history.replaceState({}, "");
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImportResult(null);
    setExtractedName(null);
    setFromMailbox(false);
    setSubmissionId(null);
    const reader = new FileReader();
    reader.onload = (ev) => processCsvText(ev.target?.result as string, file.name);
    reader.readAsText(file, "UTF-8");
  };

  const handleImport = async () => {
    if (!csvPreview?.dataRows?.length) return;
    setImporting(true);
    setImportResult(null);
    setNameErrorMsg(null);
    try {
      const payload = { fileName: csvPreview.fileName, records: csvPreview.dataRows };
      const res = await adminApi.importAttendance(payload);
      setImportResult({ type: "success", ...res.data });
      // メールボックス経由の場合、取込済みマーク
      if (submissionId !== null) {
        try { await csvMailboxApi.markImported(submissionId); } catch { /* ignore */ }
        setSubmissionId(null);
      }
      setCsvPreview(null);
      setExtractedName(null);
      setFromMailbox(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    } catch (err: unknown) {
      const errData = (err as { response?: { data?: { message?: string; errors?: string[] } } })?.response?.data;
      const msg = errData?.message ?? "インポートに失敗しました";
      // 社員名不一致はモーダルで表示
      if (msg.includes("一致する社員名がありません")) {
        setNameErrorMsg(msg);
      } else {
        setImportResult({ type: "error", successCount: 0, errorCount: 1, errors: [msg] });
      }
    } finally {
      setImporting(false);
    }
  };

  const resetImport = () => {
    setCsvPreview(null);
    setExtractedName(null);
    setImportResult(null);
    setFromMailbox(false);
    setSubmissionId(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const [y, m] = month.split("-");

  return (
    <div className="upload-page">
      <div className="breadcrumb">ホーム <span className="bc-sep">›</span> <span>CSVアップロード</span></div>
      <div className="page-header">
        <div>
          <div className="page-title">勤怠データCSVアップロード</div>
          <div className="page-subtitle">氏名で社員を識別し、勤務記録を登録します</div>
        </div>
        <input type="month" className="input month-input" value={month}
          onChange={(e) => { setMonth(e.target.value); resetImport(); }} />
      </div>

      <div className="card upload-card">
        <div className="card-title">CSVファイル選択</div>
        <div className="upload-info">
          <span className="upload-info-label">認識されるカラム:</span>
          {Object.values(FIELD_LABELS).map((label) => (
            <span key={label} className="col-tag recognized">{label}</span>
          ))}
          <span className="upload-required-note">※ 氏名はCSVヘッダーの「氏名：」欄からも自動認識されます</span>
        </div>

        {/* メールボックスから読み込んだ場合のバナー */}
        {fromMailbox && csvPreview?.fileName && (
          <div className="import-msg info">
            メールボックスより受信: <strong>{csvPreview.fileName}</strong> を読み込みました。内容を確認して登録してください。
          </div>
        )}

        <div className="upload-area">
          <label className="upload-drop-zone">
            <div className="upload-icon">↑</div>
            <div className="upload-drop-text">ここをクリックしてCSVファイルを選択</div>
            <div className="upload-drop-sub">対応形式: CSV (UTF-8 / Shift-JIS) ・ 日付形式: 4月1日(水) または YYYY-MM-DD</div>
            <input ref={fileInputRef} type="file" accept=".csv,text/csv" onChange={handleFileChange} style={{ display: "none" }} />
          </label>
          {csvPreview?.fileName && !fromMailbox && (
            <div className="upload-filename">選択済み: {csvPreview.fileName}</div>
          )}
        </div>

        {/* 氏名自動認識結果 */}
        {extractedName && (
          <div className="import-msg success">
            氏名認識: <strong>{extractedName}</strong>（ヘッダーより自動取得）
          </div>
        )}

        {csvPreview?.error && <div className="import-msg error">{csvPreview.error}</div>}
        {(csvPreview?.missingRequired?.length ?? 0) > 0 && (
          <div className="import-msg error">
            必須項目がありません: {csvPreview!.missingRequired!.map((f) => FIELD_LABELS[f] ?? f).join("、")}
          </div>
        )}
        {(csvPreview?.unknownHeaders?.length ?? 0) > 0 && (
          <div className="import-msg warn">無視されるカラム: {csvPreview!.unknownHeaders.join("、")}</div>
        )}

        {csvPreview?.dataRows?.length && !csvPreview.missingRequired?.length ? (
          <>
            <div className="import-preview-header">
              プレビュー（全 {csvPreview.dataRows.length}件 / 対象月: {y}年{m}月）
            </div>
            <div className="import-preview-wrap import-preview-scroll">
              <table className="tbl import-preview-tbl">
                <thead>
                  <tr>
                    {Object.keys(FIELD_LABELS)
                      .filter((k) => csvPreview.dataRows[0]?.[k] !== undefined)
                      .map((k) => <th key={k}>{FIELD_LABELS[k]}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {csvPreview.dataRows.slice(0, 10).map((row, i) => (
                    <tr key={i}>
                      {Object.keys(FIELD_LABELS)
                        .filter((k) => csvPreview.dataRows[0]?.[k] !== undefined)
                        .map((k) => <td key={k}>{row[k] ?? "—"}</td>)}
                    </tr>
                  ))}
                  {csvPreview.dataRows.length > 10 && (
                    <tr>
                      <td colSpan={Object.keys(FIELD_LABELS).length} style={{ textAlign: "center", color: "var(--text4)" }}>
                        ... 他 {csvPreview.dataRows.length - 10}件
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="import-actions">
              <button className="btn btn-primary" onClick={handleImport} disabled={importing}>
                {importing ? "アップロード中..." : `${csvPreview.dataRows.length}件 登録`}
              </button>
              <button className="btn btn-secondary" onClick={resetImport}>リセット</button>
            </div>
          </>
        ) : null}

        {importResult && (
          <div className={`import-msg ${importResult.type === "success" ? "success" : "error"}`}>
            {importResult.type === "success"
              ? `完了: ${importResult.successCount}件 成功 / ${importResult.errorCount}件 エラー`
              : importResult.errors[0]}
            {importResult.errors?.length > 0 && importResult.type === "success" && (
              <ul className="import-error-list">
                {importResult.errors.map((e, i) => <li key={i}>{e}</li>)}
              </ul>
            )}
          </div>
        )}
      </div>

      {/* 社員名不一致 モーダル */}
      {nameErrorMsg && (
        <div className="name-error-overlay" onClick={() => setNameErrorMsg(null)}>
          <div className="name-error-modal" onClick={(e) => e.stopPropagation()}>
            <div className="name-error-icon">⚠</div>
            <div className="name-error-title">社員名が一致しません</div>
            <div className="name-error-body">{nameErrorMsg}</div>
            <p className="name-error-hint">
              CSVに記載されている氏名と、登録済み社員名が一致しているか確認してください。
            </p>
            <button className="btn btn-primary" onClick={() => setNameErrorMsg(null)}>閉じる</button>
          </div>
        </div>
      )}
    </div>
  );
}
