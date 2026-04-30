import { isAxiosError } from "axios";

export function getErrMsg(error: unknown, fallback: string): string {
  if (isAxiosError(error)) {
    return error.response?.data?.message ?? fallback;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
