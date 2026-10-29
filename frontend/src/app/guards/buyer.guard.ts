import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';

export const buyerGuard: CanActivateFn = () =>
{
  const authService = inject(AuthService);
  const userService = inject(UserService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  return userService.getCurrentUser().pipe(
    map((user) => user.role === 'ROLE_CLIENT' || user.role === 'CLIENT'
      ? true
      : router.createUrlTree(['/profile'])),
    catchError(() => [router.createUrlTree(['/login'])])
  );
};
