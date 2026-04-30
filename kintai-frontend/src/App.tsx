import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuthContext } from "./contexts/AuthContext";
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

function AuthLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuthContext();
  return (
    <div className="app-layout">
      <Topbar user={user} onLogout={logout} />
      <div className="app-body">
        <Sidebar isAdmin={user?.role === "ADMIN"} />
        <main className="main-content">{children}</main>
      </div>
    </div>
  );
}

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isLoggedIn } = useAuthContext();
  return isLoggedIn ? <>{children}</> : <Navigate to="/login" replace />;
}

function AppRoutes() {
  const { login, isAdmin, isLoggedIn } = useAuthContext();

  const wrap = (element: React.ReactNode) => (
    <PrivateRoute>
      <AuthLayout>{element}</AuthLayout>
    </PrivateRoute>
  );

  return (
    <Routes>
      <Route path="/login"       element={<Login onLogin={login} />} />
      <Route path="/admin/login" element={<Navigate to="/login" replace />} />

      <Route path="/dashboard" element={wrap(<Dashboard />)} />
      <Route path="/monthly"   element={wrap(<MonthlyRecord />)} />

      <Route path="/admin/employees" element={wrap(<EmployeeMaster />)} />
      <Route path="/admin/monthly"   element={wrap(<AdminMonthlyRecord />)} />
      <Route path="/admin/leaves"    element={wrap(<AdminLeave />)} />
      <Route path="/admin/upload"    element={wrap(<Upload />)} />

      <Route path="*" element={
        isLoggedIn
          ? <Navigate to={isAdmin ? "/admin/monthly" : "/dashboard"} replace />
          : <Navigate to="/login" replace />
      } />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
