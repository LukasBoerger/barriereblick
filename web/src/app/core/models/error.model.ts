export interface ErrorResponse {
  status: number;
  message: string;
  fieldErrors?: Record<string, string[]>;
}
