import { TestBed } from '@angular/core/testing';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

// AC-7: the auth guard blocks a user who is not logged in, and lets a logged-in user pass.
describe('authGuard', () => {
  function run(isLoggedIn: boolean, login: () => void): boolean {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { isLoggedIn: () => isLoggedIn, login } }],
    });
    return TestBed.runInInjectionContext(() => authGuard(null as never, null as never)) as boolean;
  }

  it('blocks and starts login when no one is logged in', () => {
    const login = vi.fn();
    const result = run(false, login);
    expect(result).toBe(false);
    expect(login).toHaveBeenCalledTimes(1);
  });

  it('blocks and starts login when the token has expired (isLoggedIn() is false)', () => {
    // AuthService.isLoggedIn() already covers "no token" and "expired token" the same way
    // (angular-oauth2-oidc's hasValidAccessToken() is false in both cases).
    const login = vi.fn();
    const result = run(false, login);
    expect(result).toBe(false);
    expect(login).toHaveBeenCalledTimes(1);
  });

  it('lets a logged-in user pass without starting a login', () => {
    const login = vi.fn();
    const result = run(true, login);
    expect(result).toBe(true);
    expect(login).not.toHaveBeenCalled();
  });
});
