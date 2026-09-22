import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Route /auth/callback: the OIDC redirect target (scope item 3). The app initializer already
 * processed the code and restored the originally requested route (preserveRequestedRoute) before
 * this component can render. This is only the fallback for the rare case that did not happen.
 */
@Component({
  selector: 'app-auth-callback',
  template: `<p>Signing you in…</p>`,
})
export class AuthCallback implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    void this.router.navigateByUrl(this.auth.isLoggedIn() ? '/customers' : '/');
  }
}
