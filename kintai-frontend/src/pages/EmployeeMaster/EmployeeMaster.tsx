import { useState, useEffect } from "react";
import { adminApi } from "../../api/api";
import type {
  AccountListItem,
  CreateAccountRequest,
  CreateAccountResponse,
} from "../../types";
import "./EmployeeMaster.css";

export default function EmployeeMaster() {
  const [accounts, setAccounts] = useState<AccountListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formMsg, setFormMsg] = useState({ text: "", type: "" });
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);
  const [newAccount, setNewAccount] = useState<CreateAccountResponse | null>(
    null,
  );
  const [showPwd, setShowPwd] = useState<Record<number, boolean>>({});

  const [form, setForm] = useState<CreateAccountRequest>({
    loginId: "",
    employeeName: "",
    department: "",
  });

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await adminApi.getAccounts();
      setAccounts(res.data ?? []);
    } catch {
      setError("社員一覧の読み込みに失敗しました");
    } finally {
      setLoading(false);
    }
  };

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.loginId || !form.employeeName) {
      setFormMsg({ text: "社員番号と氏名は必須です", type: "error" });
      return;
    }
    if (form.loginId.length !== 8) {
      setFormMsg({
        text: "社員番号は正確に8桁で入力してください",
        type: "error",
      });
      return;
    }
    setSubmitting(true);
    setFormMsg({ text: "", type: "" });
    try {
      const res = await adminApi.createAccount(form);
      setNewAccount(res.data);
      setFormMsg({ text: "社員を登録しました", type: "success" });
      setForm({ loginId: "", employeeName: "", department: "" });
      setShowForm(false);
      fetchAccounts();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "登録に失敗しました";
      setFormMsg({ text: msg, type: "error" });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (accountId: number) => {
    try {
      await adminApi.deleteAccount(accountId);
      setDeleteConfirm(null);
      fetchAccounts();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "削除に失敗しました";
      setError(msg);
    }
  };

  const togglePwd = (id: number) =>
    setShowPwd((prev) => ({ ...prev, [id]: !prev[id] }));

  return (
    <div className="admin-accounts">
      <div className="breadcrumb">
        ホーム <span className="bc-sep">›</span> <span>社員管理</span>
      </div>

      <div className="page-header">
        <div>
          <div className="page-title">社員管理</div>
          <div className="page-subtitle">
            アカウント作成 / 削除・社員番号およびパスワード確認
          </div>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => {
            setShowForm(!showForm);
            setFormMsg({ text: "", type: "" });
            setNewAccount(null);
          }}
        >
          {showForm ? "キャンセル" : "+ 新規社員登録"}
        </button>
      </div>

      {/* 新規登録直後のパスワード表示 */}
      {newAccount && (
        <div className="card new-account-card">
          <div className="new-account-title">
            登録完了 - 下記情報を社員へお知らせください
          </div>
          <div className="new-account-info">
            <div className="new-account-row">
              <span className="new-account-label">社員番号（ログインID）</span>
              <span className="new-account-value mono">
                {newAccount.loginId}
              </span>
            </div>
            <div className="new-account-row">
              <span className="new-account-label">初期パスワード</span>
              <span className="new-account-value mono highlight">
                {newAccount.password}
              </span>
            </div>
          </div>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setNewAccount(null)}
          >
            閉じる
          </button>
        </div>
      )}

      {/* 新規登録フォーム */}
      {showForm && (
        <div className="card account-form-card">
          <div className="card-title">新規社員登録</div>
          <form onSubmit={handleCreate} className="account-form">
            <div className="form-row">
              <div className="form-field">
                <label className="form-label">
                  社員番号（8桁） <span className="required">*</span>
                </label>
                <input
                  className="input"
                  name="loginId"
                  value={form.loginId}
                  onChange={handleFormChange}
                  placeholder="例: 20240001"
                  maxLength={8}
                />
                <div className="form-hint">
                  ログインIDとして使用されます。パスワードは自動生成されます。
                </div>
              </div>
              <div className="form-field">
                <label className="form-label">
                  氏名 <span className="required">*</span>
                </label>
                <input
                  className="input"
                  name="employeeName"
                  value={form.employeeName}
                  onChange={handleFormChange}
                  placeholder="例: 山田太郎"
                />
              </div>
              <div className="form-field">
                <label className="form-label">部署</label>
                <input
                  className="input"
                  name="department"
                  value={form.department}
                  onChange={handleFormChange}
                  placeholder="例: 開発部"
                />
              </div>
            </div>
            {formMsg.text && (
              <div className={`form-msg ${formMsg.type}`}>{formMsg.text}</div>
            )}
            <div className="form-actions">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting}
              >
                {submitting ? "登録中..." : "登録"}
              </button>
            </div>
          </form>
        </div>
      )}

      {formMsg.text && !showForm && (
        <div
          className={`form-msg ${formMsg.type}`}
          style={{ marginBottom: "12px" }}
        >
          {formMsg.text}
        </div>
      )}

      <div className="card" style={{ marginTop: "14px" }}>
        <div className="card-title">
          社員一覧 <span className="count-badge">{accounts.length}名</span>
        </div>
        {loading ? (
          <div className="no-data">読み込み中...</div>
        ) : error ? (
          <div className="no-data" style={{ color: "var(--red-text)" }}>
            {error}
          </div>
        ) : (
          <table className="tbl">
            <thead>
              <tr>
                <th>社員番号</th>
                <th>氏名</th>
                <th>部署</th>
                <th>パスワード</th>
                <th>権限</th>
                <th>状態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {accounts.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    style={{
                      textAlign: "center",
                      color: "var(--text4)",
                      padding: "20px",
                    }}
                  >
                    登録済みの社員はいません
                  </td>
                </tr>
              ) : (
                accounts.map((a) => (
                  <tr key={a.accountId}>
                    <td>
                      <span className="code-badge">{a.loginId}</span>
                    </td>
                    <td className="name-cell">{a.employeeName}</td>
                    <td>{a.department ?? "—"}</td>
                    <td>
                      {a.role === "ADMIN" ? (
                        <span
                          style={{ color: "var(--text4)", fontSize: "12px" }}
                        >
                          —
                        </span>
                      ) : (
                        <span className="pwd-cell">
                          {showPwd[a.accountId] ? (
                            <span className="mono">
                              {a.passwordPlain ?? "—"}
                            </span>
                          ) : (
                            <span className="pwd-mask">••••••••</span>
                          )}
                          <button
                            className="btn-icon"
                            onClick={() => togglePwd(a.accountId)}
                            title={showPwd[a.accountId] ? "非表示" : "表示"}
                          >
                            {showPwd[a.accountId] ? "非表示" : "表示"}
                          </button>
                        </span>
                      )}
                    </td>
                    <td>
                      {a.role === "ADMIN" ? (
                        <span className="badge badge-admin">管理者</span>
                      ) : (
                        <span className="badge badge-user">一般</span>
                      )}
                    </td>
                    <td>
                      {a.activeFlag === 1 ? (
                        <span className="badge badge-green">有効</span>
                      ) : (
                        <span className="badge badge-gray">無効</span>
                      )}
                    </td>
                    <td>
                      {a.role === "ADMIN" ? (
                        <span
                          style={{ color: "var(--text4)", fontSize: "12px" }}
                        >
                          —
                        </span>
                      ) : deleteConfirm === a.accountId ? (
                        <div className="delete-confirm">
                          <span className="delete-warn-badge">
                            完全削除されます
                          </span>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDelete(a.accountId)}
                          >
                            削除
                          </button>
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setDeleteConfirm(null)}
                          >
                            キャンセル
                          </button>
                        </div>
                      ) : (
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => setDeleteConfirm(a.accountId)}
                        >
                          削除
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
