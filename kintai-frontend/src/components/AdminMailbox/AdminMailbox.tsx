import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { csvMailboxApi } from "../../api/api";
import type { CsvSubmission } from "../../types";
import "./AdminMailbox.css";

export default function AdminMailbox() {
  const navigate   = useNavigate();
  const [open,     setOpen]     = useState(false);
  const [items,    setItems]    = useState<CsvSubmission[]>([]);
  const [pending,  setPending]  = useState(0);
  const [filter,   setFilter]   = useState("PENDING");
  const [loading,  setLoading]  = useState(false);
  const [opening,  setOpening]  = useState<number | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  // 未処理件数ポーリング（30秒ごと）
  useEffect(() => {
    fetchPending();
    const timer = setInterval(fetchPending, 30000);
    return () => clearInterval(timer);
  }, []);

  // パネル外部クリックで閉じる
  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open]);

  const fetchPending = async () => {
    try {
      const res = await csvMailboxApi.getPendingCount();
      setPending(res.data.count ?? 0);
    } catch { /* ignore */ }
  };

  const fetchList = async (status: string) => {
    setLoading(true);
    try {
      const res = await csvMailboxApi.getList(status ? { status } : {});
      setItems(res.data ?? []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  const handleOpen = () => {
    setOpen((v) => {
      if (!v) fetchList(filter);
      return !v;
    });
  };

  const handleFilter = (status: string) => {
    setFilter(status);
    fetchList(status);
  };

  // CSVコンテンツを取得してアップロードページへ遷移
  const handleConfirm = async (item: CsvSubmission) => {
    setOpening(item.id);
    try {
      const res = await csvMailboxApi.getContent(item.id);
      setOpen(false);
      navigate("/admin/upload", {
        state: {
          csvContent:   res.data.csvContent,
          fileName:     res.data.fileName,
          submissionId: item.id,
          employeeName: res.data.employeeName,
        },
      });
    } catch {
      /* ignore */
    } finally {
      setOpening(null);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await csvMailboxApi.deleteSubmission(id);
      setItems((prev) => prev.filter((it) => it.id !== id));
      await fetchPending();
    } catch { /* ignore */ }
  };

  return (
    <div className="mailbox-wrap" ref={panelRef}>
      <button className="mailbox-btn" onClick={handleOpen} title="CSVメールボックス">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
          <polyline points="22,6 12,13 2,6"/>
        </svg>
        {pending > 0 && <span className="mailbox-badge">{pending > 99 ? "99+" : pending}</span>}
      </button>

      {open && (
        <div className="mailbox-panel">
          <div className="mailbox-panel-header">
            <span className="mailbox-panel-title">勤務表CSVメールボックス</span>
            <button className="modal-close" onClick={() => setOpen(false)}>✕</button>
          </div>

          <div className="mailbox-filter-row">
            <button
              className={`mailbox-filter-btn ${filter === "PENDING" ? "active" : ""}`}
              onClick={() => handleFilter("PENDING")}
            >
              未確認 {pending > 0 && <span className="mailbox-badge-inline">{pending}</span>}
            </button>
            <button
              className={`mailbox-filter-btn ${filter === "" ? "active" : ""}`}
              onClick={() => handleFilter("")}
            >
              全件
            </button>
          </div>

          <div className="mailbox-list">
            {loading ? (
              <div className="mailbox-empty">読み込み中...</div>
            ) : items.length === 0 ? (
              <div className="mailbox-empty">CSVの受信はありません</div>
            ) : (
              items.map((item) => (
                <div key={item.id} className={`mailbox-item ${item.status === "PENDING" ? "unread" : "read"}`}>
                  <div className="mailbox-item-top">
                    <span className="mailbox-item-name">{item.employeeName}</span>
                    <span className="mailbox-item-date">{item.submittedAt}</span>
                    {item.status === "IMPORTED" && (
                      <span className="mailbox-imported-badge">登録済</span>
                    )}
                  </div>

                  <div className="mailbox-item-meta">
                    <span className="mailbox-tag">{item.fileName}</span>
                    <span className="mailbox-tag type">約{item.recordCount}件</span>
                  </div>

                  <div className="mailbox-item-actions">
                    <button
                      className="mailbox-import-btn"
                      onClick={() => handleConfirm(item)}
                      disabled={opening === item.id}
                    >
                      {opening === item.id ? "読込中..." : "確認・取込"}
                    </button>
                    <button
                      className="mailbox-delete-btn"
                      onClick={() => handleDelete(item.id)}
                      disabled={opening === item.id}
                    >
                      削除
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
