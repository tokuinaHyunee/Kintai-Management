import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./hooks/useAuth";
import Topbar from "./components/Topbar";
import Sidebar from "./components/Sidebar";
import Login from "./pages/Login/Login";
import Dashboard from "./pages/Dashboard/Dashboard";
import MonthlyRecord from "./pages/MonthlyRecord/MonthlyRecord";
import AdminMonthlyRecord from "./pages/AdminMonthlyRecord/AdminMonthlyRecord";
import EmployeeMaster from "./pages/EmployeeMaster/EmployeeMaster";
import Upload from "./pages/Upload/Upload";
import AdminLeave from "./pages/AdminLeave/AdminLeave";
import "./styles/global.css";

interface AuthLayoutProps {
  user: ReturnType<typeof useAuth>["user"];
  onLogout: () => void;
  children: React.ReactNode;
}

function AuthLayout({ user, onLogout, children }: AuthLayoutProps) {
  return (
    <div className="app-layout">
      <Topbar user={user} onLogout={onLogout} />
      <div className="app-body">
        <Sidebar isAdmin={user?.role === "ADMIN"} />
        <main className="main-content">{children}</main>
      </div>
    </div>
  );
}

function PrivateRoute({ isLoggedIn, children }: { isLoggedIn: boolean; children: React.ReactNode }) {
  return isLoggedIn ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  const { user, login, logout, isAdmin, isLoggedIn } = useAuth();

  const wrap = (element: React.ReactNode) => (
    <PrivateRoute isLoggedIn={isLoggedIn}>
      <AuthLayout user={user} onLogout={logout}>{element}</AuthLayout>
    </PrivateRoute>
  );

  return (
    <BrowserRouter>
      <Routes>
        {/* 公開ルート */}
        <Route path="/login"       element={<Login onLogin={login} />} />
        <Route path="/admin/login" element={<Navigate to="/login" replace />} />

        {/* 一般社員ルート */}
        <Route path="/dashboard" element={wrap(<Dashboard user={user} />)} />
        <Route path="/monthly"   element={wrap(<MonthlyRecord user={user} />)} />

        {/* 管理者ルート */}
        <Route path="/admin/employees" element={wrap(<EmployeeMaster />)} />
        <Route path="/admin/monthly"   element={wrap(<AdminMonthlyRecord />)} />
        <Route path="/admin/leaves"    element={wrap(<AdminLeave />)} />
        <Route path="/admin/upload"    element={wrap(<Upload user={user} />)} />

        {/* デフォルトリダイレクト */}
        <Route path="*" element={
          isLoggedIn
            ? <Navigate to={isAdmin ? "/admin/monthly" : "/dashboard"} replace />
            : <Navigate to="/login" replace />
        } />
      </Routes>
    </BrowserRouter>
  );
}
