import { useState, useRef } from "react";
import AdminMailbox from "./AdminMailbox/AdminMailbox";
import { attendanceApi } from "../api/api";
import type { User } from "../types";
import "./Topbar.css";

interface Props {
  user: User | null;
  onLogout: () => void;
}

interface CsvFileState {
  fileName: string;
  csvContent: string;
  lineCount: number;
}

export default function Topbar({ user, onLogout }: Props) {
  const isAdmin = user?.role === "ADMIN";
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [showModal, setShowModal] = useState(false);
  const [csvFile,   setCsvFile]   = useState<CsvFileState | null>(null);
  const [sending,   setSending]   = useState(false);
  const [result,    setResult]    = useState<{ type: string; text: string } | null>(null);

  const openSendModal = () => {
    setShowModal(true);
    setCsvFile(null);
    setResult(null);
  };

  const closeSendModal = () => {
    setShowModal(false);
    setCsvFile(null);
    setResult(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setResult(null);
    const reader = new FileReader();
    reader.onload = (ev) => {
      const text = ev.target?.result as string;
      // 非空行数をカウント (ヘッダー行を除いて目安件数とする)
      const lines = text.split(/\r?\n/).filter((l) => l.trim() !== "");
      const lineCount = Math.max(0, lines.length - 1);
      setCsvFile({ fileName: file.name, csvContent: text, lineCount });
    };
    reader.readAsText(file, "UTF-8");
  };

  const handleSend = async () => {
    if (!csvFile) return;
    setSending(true);
    try {
      const res = await attendanceApi.submitCsv({
        fileName: csvFile.fileName,
        csvContent: csvFile.csvContent,
      });
      setResult({ type: "success", text: `管理者へ送信しました（約${res.data.count}件）` });
      setCsvFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "送信に失敗しました";
      setResult({ type: "error", text: msg });
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <header className="topbar">
        <div className="topbar-logo">
          勤怠<span>管理</span>システム
        </div>
        <div className="topbar-spacer" />
        <div className="topbar-user">
          <div className="topbar-avatar">
            {user?.employeeName?.charAt(0) ?? "U"}
          </div>
          <span className="topbar-name">{user?.employeeName}</span>
          {isAdmin && <span className="topbar-role-badge">管理者</span>}
          {isAdmin && <AdminMailbox />}
          {!isAdmin && (
            <button className="topbar-csv-send" onClick={openSendModal}>
              CSV送信
            </button>
          )}
          <button className="topbar-logout" onClick={onLogout}>
            ログアウト
          </button>
        </div>
      </header>

      {showModal && (
        <div className="csv-modal-overlay" onClick={closeSendModal}>
          <div className="csv-modal" onClick={(e) => e.stopPropagation()}>
            <div className="csv-modal-header">
              <span className="csv-modal-title">勤務表CSV送信（管理者へ送付）</span>
              <button className="csv-modal-close" onClick={closeSendModal}>✕</button>
            </div>
            <div className="csv-modal-body">
              <p className="csv-modal-desc">
                勤務表CSVファイルを管理者へ送信します。管理者がCSVアップロードページで内容を確認・登録します。
              </p>

              <label className="csv-modal-upload">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".csv,text/csv"
                  onChange={handleFileChange}
                  style={{ display: "none" }}
                />
                {csvFile ? (
                  <span className="csv-modal-filename">選択済み: {csvFile.fileName}</span>
                ) : (
                  <span>CSVファイルを選択</span>
                )}
              </label>

              {csvFile && (
                <div className="csv-modal-info">
                  約{csvFile.lineCount}行 検出
                </div>
              )}

              {result && (
                <div className={`csv-modal-msg ${result.type}`}>{result.text}</div>
              )}
            </div>
            <div className="csv-modal-footer">
              <button className="btn btn-secondary btn-sm" onClick={closeSendModal}>閉じる</button>
              <button
                className="btn btn-primary btn-sm"
                onClick={handleSend}
                disabled={sending || !csvFile}
              >
                {sending ? "送信中..." : "送信"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
