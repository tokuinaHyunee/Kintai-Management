import { useState, useCallback, useEffect, useRef } from "react";
import { authApi } from "../api/api";
import type { User } from "../types";

interface AuthState {
  user: User | null;
  login: (token: string, userInfo: User) => void;
  logout: () => void;
  isAdmin: boolean;
  isLoggedIn: boolean;
}

const INACTIVE_LIMIT_MS = 30 * 60 * 1000;  // 30分非活動 → 自動ログアウト
const REFRESH_INTERVAL_MS = 25 * 60 * 1000; // 25分ごとにトークン更新

// 認証状態管理フック
// JWT・ユーザー情報をlocalStorageと同期して管理する
export function useAuth(): AuthState {
  const [user, setUser] = useState<User | null>(() => {
    try {
      const saved = localStorage.getItem("user");
      return saved ? (JSON.parse(saved) as User) : null;
    } catch {
      return null;
    }
  });

  const lastActivityRef = useRef<number>(0);

  const login = useCallback((token: string, userInfo: User) => {
    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(userInfo));
    lastActivityRef.current = Date.now();
    setUser(userInfo);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  }, []);

  // 非活動検知 + トークン自動更新
  useEffect(() => {
    if (!user) return;

    lastActivityRef.current = Date.now();

    const updateActivity = () => {
      lastActivityRef.current = Date.now();
    };

    const events = ["mousemove", "keydown", "click", "touchstart", "scroll"] as const;
    events.forEach((e) => window.addEventListener(e, updateActivity, { passive: true }));

    // 1分ごとに非活動チェック → 30分超過で自動ログアウト
    const inactivityTimer = setInterval(() => {
      if (Date.now() - lastActivityRef.current > INACTIVE_LIMIT_MS) {
        logout();
        window.location.href = "/login";
      }
    }, 60_000);

    // 25分ごとにトークン更新（活動中の場合のみ）
    const refreshTimer = setInterval(async () => {
      if (Date.now() - lastActivityRef.current < INACTIVE_LIMIT_MS) {
        try {
          const res = await authApi.refresh();
          localStorage.setItem("token", res.data.token);
        } catch {
          logout();
          window.location.href = "/login";
        }
      }
    }, REFRESH_INTERVAL_MS);

    return () => {
      events.forEach((e) => window.removeEventListener(e, updateActivity));
      clearInterval(inactivityTimer);
      clearInterval(refreshTimer);
    };
  }, [user, logout]);

  const isAdmin    = user?.role === "ADMIN";
  const isLoggedIn = !!user;

  return { user, login, logout, isAdmin, isLoggedIn };
}
