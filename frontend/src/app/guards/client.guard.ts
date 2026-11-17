import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { UserContextService } from '../services/user-context-service';

export const clientGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userContext = inject(UserContextService);
  const currentUser = userContext.user();

  if (!localStorage.getItem('auth_token')) {
    return router.parseUrl('/login');
  }

  if (currentUser) {
    return currentUser.role === 'CLIENT' ? true : router.parseUrl('/');
  }

  return userContext.load().pipe(
    map(() => {
      const loadedUser = userContext.user();
      if (!loadedUser) return router.parseUrl('/login');
      return loadedUser.role === 'CLIENT' ? true : router.parseUrl('/');
    }),
  );
};
