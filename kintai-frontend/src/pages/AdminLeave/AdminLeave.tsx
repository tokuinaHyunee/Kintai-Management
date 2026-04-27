import { useState, useEffect } from "react";
import { adminLeaveApi } from "../../api/api";
import type { LeaveRequest, LeaveStatus } from "../../types";
import { getErrMsg } from "../../utils/error";
import { STATUS_BADGE } from "../../utils/labels";
import "./AdminLeave.css";

export default function AdminLeave() {
  const [leaves,       setLeaves]       = useState<LeaveRequest[]>([]);
  const [filter,       setFilter]       = useState<LeaveStatus | "ALL">("PENDING");
  const [loading,      setLoading]      = useState(false);
  const [actionMsg,    setActionMsg]    = useState({ text: "", isError: false });
  const [rejectTarget, setRejectTarget] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [submitting,   setSubmitting]   = useState(false);

  useEffect(() => { fetchLeaves(); }, [filter]);

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const params = filter !== "ALL" ? { status: filter } : undefined;
      const res = await adminLeaveApi.getAll(params);
      setLeaves(res.data ?? []);
    } catch {
      setLeaves([]);
    } finally {
      setLoading(false);
    }
  };

  const showMsg = (text: string, isError: boolean) => {
    setActionMsg({ text, isError });
    setTimeout(() => setActionMsg({ text: "", isError: false }), 4000);
  };

  const handleApprove = async (id: number) => {
    setSubmitting(true);
    try {
      await adminLeaveApi.approve(id);
      showMsg("承認しました", false);
      fetchLeaves();
    } catch (e) {
      showMsg(getErrMsg(e, "承認に失敗しました"), true);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!rejectTarget) return;
    if (!rejectReason.trim()) {
      showMsg("却下理由を入力してください", true);
      return;
    }
    setSubmitting(true);
    try {
      await adminLeaveApi.reject(rejectTarget, rejectReason.trim());
      showMsg("却下しました", false);
      setRejectTarget(null);
      setRejectReason("");
      fetchLeaves();
    } catch (e) {
      showMsg(getErrMsg(e, "却下に失敗しました"), true);
    } finally {
      setSubmitting(false);
    }
  };

  const openReject = (id: number) => {
    setRejectTarget(id);
    setRejectReason("");
  };

  const pendingCount = leaves.filter((l) => l.status === "PENDING").length;

  return (
    <div className="admin-leave">
      <div className="breadcrumb">ホーム <span className="bc-sep">›</span> <span>休暇申請管理</span></div>
      <div className="page-header">
        <div>
          <div className="page-title">休暇申請管理</div>
          <div className="page-subtitle">社員からの休暇申請を確認・承認・却下できます</div>
        </div>
        {filter === "PENDING" && pendingCount > 0 && (
          <div className="pending-badge-large">{pendingCount}件 未処理</div>
        )}
      </div>

      {actionMsg.text && (
        <div className={`action-msg ${actionMsg.isError ? "action-msg-error" : "action-msg-success"}`}>
          {actionMsg.text}
        </div>
      )}

      {/* 却下モーダル */}
      {rejectTarget !== null && (
        <div className="modal-overlay" onClick={() => setRejectTarget(null)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-title">却下理由の入力</div>
            <textarea
              className="input reject-reason-input"
              placeholder="却下理由を入力してください（必須）"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              maxLength={500}
              autoFocus
            />
            <div className="modal-actions">
              <button
                className="btn btn-danger"
                onClick={handleRejectSubmit}
                disabled={submitting || !rejectReason.trim()}
              >
                {submitting ? "処理中..." : "却下する"}
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => setRejectTarget(null)}
                disabled={submitting}
              >
                キャンセル
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="card" style={{ marginTop: "14px" }}>
        <div className="card-title" style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          <span>休暇申請一覧</span>
          <div className="filter-tabs" style={{ marginLeft: "auto" }}>
            {(["PENDING", "APPROVED", "REJECTED", "ALL"] as const).map((s) => (
              <button
                key={s}
                className={`filter-tab ${filter === s ? "active" : ""}`}
                onClick={() => setFilter(s)}
              >
                {s === "PENDING" ? "未処理" : s === "APPROVED" ? "承認済み" : s === "REJECTED" ? "却下" : "全件"}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="no-data">読み込み中...</div>
        ) : leaves.length === 0 ? (
          <div className="no-data">申請がありません</div>
        ) : (
          <table className="tbl">
            <thead>
              <tr>
                <th>申請日</th>
                <th>社員名</th>
                <th>休暇種別</th>
                <th>取得日</th>
                <th>申請理由</th>
                <th>ステータス</th>
                <th>審査日</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {leaves.map((r) => (
                <tr key={r.id}>
                  <td>{r.createdAt}</td>
                  <td className="name-cell">{r.employeeName}</td>
                  <td>{r.leaveTypeName}</td>
                  <td>{r.leaveDate}</td>
                  <td className="memo-cell">{r.reason ?? "—"}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGE[r.status] ?? "badge-gray"}`}>
                      {r.statusName}
                    </span>
                    {r.rejectReason && (
                      <div className="reject-reason-display">{r.rejectReason}</div>
                    )}
                  </td>
                  <td>{r.reviewedAt ?? "—"}</td>
                  <td>
                    {r.status === "PENDING" ? (
                      <div className="action-btns">
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => handleApprove(r.id)}
                          disabled={submitting}
                        >
                          承認
                        </button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => openReject(r.id)}
                          disabled={submitting}
                        >
                          却下
                        </button>
                      </div>
                    ) : (
                      <span style={{ color: "var(--text4)", fontSize: "12px" }}>—</span>
                    )}
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
