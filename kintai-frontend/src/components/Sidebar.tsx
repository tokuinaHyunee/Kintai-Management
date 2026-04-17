import { useNavigate, useLocation } from "react-router-dom";
import "./Sidebar.css";

interface MenuItem {
  label: string;
  path: string;
  icon: string;
}

// 一般社員メニュー: 出退勤 + 月次集計のみ
const MENU_ITEMS: MenuItem[] = [
  { label: "出退勤",   path: "/dashboard", icon: "▪" },
  { label: "月次集計", path: "/monthly",   icon: "▪" },
];

// 管理者メニュー: 社員管理 / 全社員月次実績 / CSVアップロード
const ADMIN_MENU_ITEMS: MenuItem[] = [
  { label: "社員管理",        path: "/admin/employees", icon: "▪" },
  { label: "全社員月次実績",  path: "/admin/monthly",   icon: "▪" },
  { label: "CSVアップロード", path: "/admin/upload",    icon: "▪" },
];

interface Props {
  isAdmin: boolean;
}

export default function Sidebar({ isAdmin }: Props) {
  const navigate = useNavigate();
  const location = useLocation();
  const items    = isAdmin ? ADMIN_MENU_ITEMS : MENU_ITEMS;

  return (
    <aside className="sidebar">
      <div className="sidebar-section">メニュー</div>
      {items.map((item) => (
        <div
          key={item.path}
          className={`sidebar-item ${location.pathname === item.path ? "active" : ""}`}
          onClick={() => navigate(item.path)}
        >
          <span className="sidebar-icon">{item.icon}</span>
          {item.label}
        </div>
      ))}
    </aside>
  );
}
