import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * AC-7: blocks a user who is not logged in (or whose token expired), and lets a logged-in user
 * pass. Not logged in starts the login redirect instead of showing a route.
 */
export const authGuard: CanActivateFn = (): boolean => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) {
    return true;
  }
  auth.login();
  return false;
};
