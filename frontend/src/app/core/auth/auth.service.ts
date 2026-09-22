import { Injectable, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { APP_CONFIG } from '../config/app-config';

interface IdentityClaims {
  readonly name?: string;
  readonly preferred_username?: string;
}

/**
 * Wraps OAuthService: configure, login, callback (via `initialize`, called once by an app
 * initializer), logout, the current user, and the access token. AC-11: the access token stays in
 * OAuthService's memory store (see SignInStateStorage), never in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private readonly config = inject(APP_CONFIG);

  /**
   * Configures the library, then processes an OIDC callback if the current URL carries one
   * (Authorization Code + PKCE). Must run once, before the app renders any guarded route.
   */
  async initialize(): Promise<void> {
    this.oauthService.configure({
      issuer: this.config.oidc.authority,
      clientId: this.config.oidc.clientId,
      redirectUri: `${window.location.origin}/auth/callback`,
      postLogoutRedirectUri: `${window.location.origin}/`,
      responseType: 'code',
      scope: 'openid profile email',
      strictDiscoveryDocumentValidation: true,
      // Deep linking for code flow: the route requested before login is restored after it.
      preserveRequestedRoute: true,
    });
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();
  }

  isLoggedIn(): boolean {
    return this.oauthService.hasValidIdToken() && this.oauthService.hasValidAccessToken();
  }

  /** Starts the Authorization Code + PKCE redirect to Keycloak. */
  login(): void {
    this.oauthService.initCodeFlow();
  }

  /** Ends the Keycloak session too (id_token_hint + postLogoutRedirectUri). */
  logout(): void {
    this.oauthService.logOut();
  }

  /** The access token, or null when there is none or it has expired. */
  getAccessToken(): string | null {
    return this.oauthService.hasValidAccessToken() ? this.oauthService.getAccessToken() : null;
  }

  /** The user's display name, from the ID token claims. */
  userName(): string | null {
    const claims = this.oauthService.getIdentityClaims() as IdentityClaims | null;
    return claims?.name ?? claims?.preferred_username ?? null;
  }
}
