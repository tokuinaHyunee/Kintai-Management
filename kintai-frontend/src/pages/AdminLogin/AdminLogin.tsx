import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../../api/api";
import type { User } from "../../types";
import "./AdminLogin.css";

interface Props {
  onLogin: (token: string, userInfo: User) => void;
}

export default function AdminLogin({ onLogin }: Props) {
  const navigate = useNavigate();
  const [loginId,  setLoginId]  = useState("");
  const [password, setPassword] = useState("");
  const [error,    setError]    = useState("");
  const [loading,  setLoading]  = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginId || !password) {
      setError("管理者IDとパスワードを入力してください");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const res = await authApi.login(loginId, password);
      const { token, employeeId, employeeName, role } = res.data;
      if (role !== "ADMIN") {
        setError("管理者アカウントではありません");
        return;
      }
      onLogin(token, { employeeId, loginId, employeeName, role });
      navigate("/admin/monthly");
    } catch {
      setError("管理者IDまたはパスワードが正しくありません");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-bg admin-login-bg">
      <div className="login-card">
        <div className="login-logo">
          <div className="login-logo-mark admin-logo-mark">
            <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
              <path d="M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z" />
              <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" strokeLinecap="round" />
              <circle cx="19" cy="8" r="3" fill="#fff" stroke="none" />
              <path d="M17.5 8h3M19 6.5v3" stroke="#1a3a51" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </div>
          <div className="login-logo-text">管理者ログイン</div>
        </div>
        <div className="login-subtitle">勤怠管理システム — Administrator</div>

        <form onSubmit={handleSubmit}>
          <div className="login-field">
            <label className="login-label">管理者ID</label>
            <input
              className="login-input"
              type="text"
              placeholder="管理者IDを入力"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              disabled={loading}
            />
          </div>
          <div className="login-field">
            <label className="login-label">パスワード</label>
            <input
              className="login-input"
              type="password"
              placeholder="パスワードを入力"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          {error && <div className="login-error">{error}</div>}

          <button className="login-btn admin-login-btn" type="submit" disabled={loading}>
            {loading ? "ログイン中..." : "管理者ログイン"}
          </button>
        </form>

        <div className="login-note">
          <span className="admin-link" onClick={() => navigate("/login")}>← 社員ログインへ戻る</span>
        </div>
      </div>
    </div>
  );
}
