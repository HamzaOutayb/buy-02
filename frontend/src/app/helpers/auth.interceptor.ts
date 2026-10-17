import { HttpInterceptorFn } from '@angular/common/http';
import { API_BASE_URL, AUTH_TOKEN_KEY } from './api-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(API_BASE_URL)) {
    return next(request);
  }

  const token = localStorage.getItem(AUTH_TOKEN_KEY);

  if (!token) {
    return next(request);
  }

  const authenticatedRequest = request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authenticatedRequest);
};
