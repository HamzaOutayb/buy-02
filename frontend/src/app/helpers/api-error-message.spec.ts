/// <reference types="jasmine" />

import { HttpErrorResponse } from '@angular/common/http';
import { getApiErrorMessage } from './api-error-message';

describe('getApiErrorMessage', () => {
  it('returns the message from the server', () => {
    const error = new HttpErrorResponse({ error: { message: 'Invalid login' } });

    expect(getApiErrorMessage(error, 'Something went wrong')).toBe('Invalid login');
  });

  it('returns the fallback when the server has no message', () => {
    const error = new HttpErrorResponse({ error: {} });

    expect(getApiErrorMessage(error, 'Something went wrong')).toBe('Something went wrong');
  });
});