export type Role = "ADMIN" | "USER";

export interface User {
  employeeId: number;
  loginId: string;
  employeeName: string;
  role: Role;
}

export interface WorkTimeRecord {
  workDate: string;
  startTime: string | null;
  endTime: string | null;
  outTime?: string | null;
  returnTime?: string | null;
  workMinutes: number | null;
  overtimeMinutes: number | null;
  workMemo?: string | null;
  leaveType?: string | null;
}

export interface MonthlySummary {
  workDays: number;
  totalHours: number;
  overtimeHours: number;
  paidLeaveDays: number;
}

export interface EmployeeMonthlySummary {
  loginId: string;
  employeeName: string;
  department: string | null;
  workDays: number;
  totalHours: number;
  overtimeHours: number;
}

export interface AccountListItem {
  accountId: number;
  employeeName: string;
  department: string | null;
  loginId: string;
  passwordPlain: string;
  role: Role;
  activeFlag: number;
}

export interface CreateAccountRequest {
  loginId: string;
  employeeName: string;
  department: string;
}

export interface CreateAccountResponse {
  message: string;
  loginId: string;
  password: string;
}

// 社員が管理者に送信したCSV提出
export type CsvSubmissionStatus = "PENDING" | "IMPORTED";

export interface CsvSubmission {
  id: number;
  employeeName: string;
  fileName: string;
  recordCount: number;
  status: CsvSubmissionStatus;
  submittedAt: string;
}

// 管理者CSV取込結果
export interface CsvImportResult {
  successCount: number;
  errorCount: number;
  message: string;
}

export interface ImportResult {
  successCount: number;
  errorCount: number;
  errors: string[];
}

export interface CsvPreview {
  rawHeaders: string[];
  mappedHeaders: (string | null)[];
  unknownHeaders: string[];
  missingRequired?: string[];
  dataRows: Record<string, string>[];
  fileName: string;
  nameError?: string | null;
  error?: string;
}

export interface WeekSummary {
  start: string;
  records: WorkTimeRecord[];
}
