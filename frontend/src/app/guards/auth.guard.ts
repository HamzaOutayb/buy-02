import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { UserContextService } from '../services/user-context-service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userContext = inject(UserContextService);

  if (!localStorage.getItem('auth_token')) {
    return router.parseUrl('/login');
  }

  const currentUser = userContext.user();
  if (currentUser) {
    return true;
  }

  return userContext.load().pipe(
    map(() => {
      const loadedUser = userContext.user();
      if (!loadedUser) {
        return router.parseUrl('/login');
      }
      return true;
    }),
  );
};
