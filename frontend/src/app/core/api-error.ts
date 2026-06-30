export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: ApiFieldError[];
}
