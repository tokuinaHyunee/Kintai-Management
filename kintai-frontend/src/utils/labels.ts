import type { LeaveType } from "../types";

export const LEAVE_TYPE_OPTIONS: { value: LeaveType; label: string }[] = [
  { value: "ANNUAL",         label: "年次有給休暇" },
  { value: "HALF_MORNING",   label: "午前半休" },
  { value: "HALF_AFTERNOON", label: "午後半休" },
  { value: "SICK",           label: "病気休暇" },
];

export const STATUS_BADGE: Record<string, string> = {
  PENDING:  "badge-blue",
  APPROVED: "badge-green",
  REJECTED: "badge-red",
};
