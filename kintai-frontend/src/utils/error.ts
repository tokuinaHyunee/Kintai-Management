export function getErrMsg(error: unknown, fallback: string): string {
  const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  if (msg) return msg;
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
