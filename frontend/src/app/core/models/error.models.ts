export interface ApiErrorDetail {
  field?: string;
  message?: string;
}

export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
  details?: ApiErrorDetail[];
}
