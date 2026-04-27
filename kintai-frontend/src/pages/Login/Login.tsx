import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../../api/api";
import type { User } from "../../types";
import "./Login.css";

interface Props {
  onLogin: (token: string, userInfo: User) => void;
}

export default function Login({ onLogin }: Props) {
  const navigate = useNavigate();
  const [loginId,  setLoginId]  = useState("");
  const [password, setPassword] = useState("");
  const [error,    setError]    = useState("");
  const [loading,  setLoading]  = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginId || !password) {
      setError("IDとパスワードを入力してください");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const res = await authApi.login(loginId, password);
      const { token, employeeId, employeeName, role } = res.data;
      onLogin(token, { employeeId, loginId, employeeName, role });
      navigate(role === "ADMIN" ? "/admin/monthly" : "/dashboard");
    } catch {
      setError("IDまたはパスワードが正しくありません");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-bg">
      <div className="login-card">
        <div className="login-logo">
          <div className="login-logo-mark">
            <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
              <rect x="3" y="4" width="18" height="14" rx="2" />
              <path d="M8 20h8M12 18v2" strokeLinecap="round" />
              <path d="M7 9h10M7 12h6" strokeLinecap="round" />
            </svg>
          </div>
          <div className="login-logo-text">勤怠管理システム</div>
        </div>
        <div className="login-subtitle">Attendance Management System</div>

        <form onSubmit={handleSubmit}>
          <div className="login-field">
            <label className="login-label">ID</label>
            <input
              className="login-input"
              type="text"
              placeholder="IDを入力"
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

          <button className="login-btn" type="submit" disabled={loading}>
            {loading ? "ログイン中..." : "ログイン"}
          </button>
        </form>

        <div className="login-note">アカウントは管理者よりご連絡いたします</div>
      </div>
    </div>
  );
}
