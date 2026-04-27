import { useState } from "react";
import { correctionApi } from "../../api/api";
import type { WorkTimeRecord, RequestType } from "../../types";
import "./CorrectionRequestModal.css";

interface Props {
  record: WorkTimeRecord;
  onClose: () => void;
  onSubmitted?: () => void;
}

const TYPE_OPTIONS: { value: RequestType; label: string }[] = [
  { value: "START_TIME", label: "出勤時刻" },
  { value: "END_TIME",   label: "退勤時刻" },
  { value: "BOTH",       label: "出退勤両方" },
];

export default function CorrectionRequestModal({ record, onClose, onSubmitted }: Props) {
  const [requestType,   setRequestType]   = useState<RequestType>("START_TIME");
  const [newStartTime,  setNewStartTime]  = useState(record.startTime ?? "");
  const [newEndTime,    setNewEndTime]    = useState(record.endTime   ?? "");
  const [reason,        setReason]        = useState("");
  const [submitting,    setSubmitting]    = useState(false);
  const [error,         setError]         = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!reason.trim()) { setError("申請理由を入力してください"); return; }
    if ((requestType === "START_TIME" || requestType === "BOTH") && !newStartTime) {
      setError("修正後の出勤時刻を入力してください"); return;
    }
    if ((requestType === "END_TIME" || requestType === "BOTH") && !newEndTime) {
      setError("修正後の退勤時刻を入力してください"); return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await correctionApi.create({
        workDate:     record.workDate,
        requestType,
        newStartTime: (requestType === "START_TIME" || requestType === "BOTH") ? newStartTime : null,
        newEndTime:   (requestType === "END_TIME"   || requestType === "BOTH") ? newEndTime   : null,
        reason,
      });
      onSubmitted?.();
      onClose();
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? "申請に失敗しました");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">修正申請</span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="modal-body">
          <div className="modal-row">
            <label className="modal-label">勤務日</label>
            <span className="modal-value-static">{record.workDate}</span>
          </div>

          <div className="modal-row">
            <label className="modal-label">修正種別</label>
            <div className="modal-type-group">
              {TYPE_OPTIONS.map((opt) => (
                <label key={opt.value} className="modal-radio">
                  <input
                    type="radio"
                    name="requestType"
                    value={opt.value}
                    checked={requestType === opt.value}
                    onChange={() => setRequestType(opt.value)}
                  />
                  {opt.label}
                </label>
              ))}
            </div>
          </div>

          {(requestType === "START_TIME" || requestType === "BOTH") && (
            <div className="modal-row">
              <label className="modal-label">修正後 出勤</label>
              <input
                type="time"
                className="input modal-input"
                value={newStartTime}
                onChange={(e) => setNewStartTime(e.target.value)}
              />
            </div>
          )}

          {(requestType === "END_TIME" || requestType === "BOTH") && (
            <div className="modal-row">
              <label className="modal-label">修正後 退勤</label>
              <input
                type="time"
                className="input modal-input"
                value={newEndTime}
                onChange={(e) => setNewEndTime(e.target.value)}
              />
            </div>
          )}

          <div className="modal-row modal-row-col">
            <label className="modal-label">申請理由</label>
            <textarea
              className="input modal-textarea"
              placeholder="修正理由を入力してください"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
            />
          </div>

          {error && <div className="modal-error">{error}</div>}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary btn-sm" onClick={onClose}>キャンセル</button>
          <button className="btn btn-primary btn-sm" onClick={handleSubmit} disabled={submitting}>
            {submitting ? "送信中..." : "申請する"}
          </button>
        </div>
      </div>
    </div>
  );
}
