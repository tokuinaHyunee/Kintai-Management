import axios, { AxiosResponse } from "axios";
import type {
  User,
  WorkTimeRecord,
  MonthlySummary,
  EmployeeMonthlySummary,
  AccountListItem,
  CreateAccountRequest,
  CreateAccountResponse,
  CsvSubmission,
  ImportResult,
  LeaveRequest,
  CreateLeaveRequest,
} from "../types";

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

// JWTトークン自動付与
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401: 自動ログアウト / ネットワークエラー: 分かりやすいメッセージ
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url: string = error.config?.url ?? "";
    if (error.response?.status === 401 && !url.includes("/auth/")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    if (error.response?.status === 503 || error.code === "ERR_NETWORK") {
      return Promise.reject(new Error("サーバーに接続できません。サーバーが起動中か確認してください。"));
    }
    return Promise.reject(error);
  },
);

// 認証
export const authApi = {
  login: (loginId: string, password: string): Promise<AxiosResponse<User & { token: string }>> =>
    api.post("/auth/login", { loginId, password }),
  refresh: (): Promise<AxiosResponse<{ token: string; expiresIn: number }>> =>
    api.post("/auth/refresh"),
};

// 勤怠
export const attendanceApi = {
  clockIn:     (): Promise<AxiosResponse<void>> => api.post("/attendance/clock-in", {}),
  clockOut:    (): Promise<AxiosResponse<void>> => api.post("/attendance/clock-out", {}),
  goOut:       (): Promise<AxiosResponse<void>> => api.post("/attendance/go-out", {}),
  goOutReturn: (): Promise<AxiosResponse<void>> => api.post("/attendance/go-out-return", {}),
  saveMemo: (data: { work_date: string; memo: string }): Promise<AxiosResponse<void>> =>
    api.post("/attendance/work-memo", data),
  getList: (params: { month: string }): Promise<AxiosResponse<WorkTimeRecord[]>> =>
    api.get("/attendance", { params }),
  // 社員 → 管理者メールボックスへCSV原本ファイル送信
  submitCsv: (data: { fileName: string; csvContent: string }): Promise<AxiosResponse<{ message: string; count: number }>> =>
    api.post("/attendance/submit-csv", data),
};

// 集計
export const summaryApi = {
  getMonthly: (params: { month: string }): Promise<AxiosResponse<MonthlySummary>> =>
    api.get("/summary/monthly", { params }),
  getWeekly: (params: { month: string }): Promise<AxiosResponse<MonthlySummary>> =>
    api.get("/summary/weekly", { params }),
  exportCsv: (month: string): Promise<AxiosResponse<Blob>> =>
    api.get("/summary/export", { params: { month }, responseType: "blob" }),
};

// 管理者
export const adminApi = {
  getMonthlySummary: (params: { month: string }): Promise<AxiosResponse<EmployeeMonthlySummary[]>> =>
    api.get("/admin/monthly-summary", { params }),
  getAccounts:   (): Promise<AxiosResponse<AccountListItem[]>> =>
    api.get("/admin/accounts"),
  createAccount: (data: CreateAccountRequest): Promise<AxiosResponse<CreateAccountResponse>> =>
    api.post("/admin/accounts", data),
  deleteAccount: (id: number): Promise<AxiosResponse<{ message: string }>> =>
    api.delete(`/admin/accounts/${id}`),
  importAttendance: (data: { fileName: string; records: Record<string, string>[] }): Promise<AxiosResponse<ImportResult>> =>
    api.post("/admin/import/attendance", data),
};

// 管理者CSVメールボックス
export const csvMailboxApi = {
  getPendingCount: (): Promise<AxiosResponse<{ count: number }>> =>
    api.get("/admin/csv-submissions/pending-count"),
  getList: (params?: { status?: string }): Promise<AxiosResponse<CsvSubmission[]>> =>
    api.get("/admin/csv-submissions", { params }),
  getContent: (id: number): Promise<AxiosResponse<{ id: number; fileName: string; csvContent: string; employeeName: string; status: string }>> =>
    api.get(`/admin/csv-submissions/${id}/content`),
  markImported: (id: number): Promise<AxiosResponse<{ message: string }>> =>
    api.post(`/admin/csv-submissions/${id}/mark-imported`),
  deleteSubmission: (id: number): Promise<AxiosResponse<{ message: string }>> =>
    api.delete(`/admin/csv-submissions/${id}`),
};

// 休暇申請（社員向け）
export const leaveApi = {
  apply: (data: CreateLeaveRequest): Promise<AxiosResponse<{ message: string }>> =>
    api.post("/leaves", data),
  getMyLeaves: (): Promise<AxiosResponse<LeaveRequest[]>> =>
    api.get("/leaves"),
};

// 休暇申請（管理者向け）
export const adminLeaveApi = {
  getAll: (params?: { status?: string }): Promise<AxiosResponse<LeaveRequest[]>> =>
    api.get("/admin/leaves", { params }),
  getPendingCount: (): Promise<AxiosResponse<{ count: number }>> =>
    api.get("/admin/leaves/pending-count"),
  approve: (id: number): Promise<AxiosResponse<{ message: string }>> =>
    api.put(`/admin/leaves/${id}/approve`),
  reject: (id: number, rejectReason: string): Promise<AxiosResponse<{ message: string }>> =>
    api.put(`/admin/leaves/${id}/reject`, { rejectReason }),
};

export default api;
