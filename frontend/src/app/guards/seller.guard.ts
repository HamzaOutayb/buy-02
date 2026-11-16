import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { UserContextService } from '../services/user-context-service';

// This guard checks if the user is authenticated and has the role of 'SELLER'.
export const sellerGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userContext = inject(UserContextService);
  const currentUser = userContext.user();

  if (!localStorage.getItem('auth_token')) {
    return router.parseUrl('/login');
  }

  if (currentUser) {
    return currentUser.role === 'SELLER' ? true : router.parseUrl('/');
  }

  return userContext.load().pipe(
    map(() => {
      const loadedUser = userContext.user();
      if (!loadedUser) {
        return router.parseUrl('/login');
      }
      return loadedUser.role === 'SELLER' ? true : router.parseUrl('/');
    }),
  );
};
