import { HttpErrorResponse } from '@angular/common/http';

export function getApiErrorMessage(error: HttpErrorResponse, fallback: string): string
{
  if (error.error && typeof error.error.message === 'string') {
    return error.error.message;
  }

  return fallback;
}
