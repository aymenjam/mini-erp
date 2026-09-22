import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { OAuthService, OAuthStorage, provideOAuthClient } from 'angular-oauth2-oidc';
import { SignInStateStorage } from './sign-in-state-storage';

// AC-11: no token in localStorage. Real angular-oauth2-oidc objects, configured as the app does.
describe('AC-11: token storage with angular-oauth2-oidc', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideOAuthClient(),
        { provide: OAuthStorage, useClass: SignInStateStorage },
      ],
    });
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('keeps the PKCE verifier and the nonce in sessionStorage, and nothing in localStorage, during a sign-in redirect', async () => {
    const oauthService = TestBed.inject(OAuthService);
    let redirected: () => void;
    const redirectedToKeycloak = new Promise<void>((resolve) => (redirected = resolve));
    oauthService.configure({
      issuer: 'http://localhost:8080/realms/mini-erp',
      clientId: 'erp-frontend',
      redirectUri: 'http://localhost:4200/auth/callback',
      loginUrl: 'http://localhost:8080/realms/mini-erp/protocol/openid-connect/auth',
      responseType: 'code',
      // Stand-in for the real browser redirect (login.ts would set location.href).
      openUri: () => redirected(),
    });

    oauthService.initCodeFlow();
    await redirectedToKeycloak;

    expect(sessionStorage.getItem('PKCE_verifier')).toBeTruthy();
    expect(sessionStorage.getItem('nonce')).toBeTruthy();
    expect(localStorage.length).toBe(0);
  });

  it('keeps a received access token out of localStorage and sessionStorage, but returns it from the service', async () => {
    const oauthService = TestBed.inject(OAuthService);
    const httpTesting = TestBed.inject(HttpTestingController);
    let redirected: () => void;
    const redirectedToKeycloak = new Promise<void>((resolve) => (redirected = resolve));
    oauthService.configure({
      issuer: 'http://localhost:8080/realms/mini-erp',
      clientId: 'erp-frontend',
      redirectUri: 'http://localhost:4200/auth/callback',
      loginUrl: 'http://localhost:8080/realms/mini-erp/protocol/openid-connect/auth',
      tokenEndpoint: 'http://localhost:8080/realms/mini-erp/protocol/openid-connect/token',
      responseType: 'code',
      openUri: () => redirected(),
    });

    // Start a sign-in so a real PKCE verifier and nonce exist (needed for the state check below).
    oauthService.initCodeFlow();
    await redirectedToKeycloak;
    const nonce = sessionStorage.getItem('nonce');

    // Simulate the browser back on /auth/callback?code=...&state=<nonce> (no id_token: the token
    // response below only carries an access token, which is enough for this check).
    history.replaceState(null, '', `/auth/callback?code=fake-code&state=${encodeURIComponent(nonce!)}`);
    const loginProcessed = oauthService.tryLoginCodeFlow();

    const tokenRequest = httpTesting.expectOne('http://localhost:8080/realms/mini-erp/protocol/openid-connect/token');
    const sentinelToken = 'sentinel-access-token-for-this-test';
    tokenRequest.flush({ access_token: sentinelToken, token_type: 'Bearer', expires_in: 300 });
    await loginProcessed;

    expect(localStorage.length).toBe(0);
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i)!;
      expect(sessionStorage.getItem(key)).not.toBe(sentinelToken);
    }
    expect(oauthService.hasValidAccessToken()).toBe(true);
    expect(oauthService.getAccessToken()).toBe(sentinelToken);
  });
});

// AC-11: SignInStateStorage on its own, so a library upgrade that adds an unknown sign-in key is
// caught by this test (it would default to memory, not silently reach sessionStorage/localStorage).
describe('SignInStateStorage', () => {
  let storage: SignInStateStorage;

  beforeEach(() => {
    sessionStorage.clear();
    storage = new SignInStateStorage();
  });

  afterEach(() => sessionStorage.clear());

  it.each(['nonce', 'PKCE_verifier', 'requested_route'])('routes the sign-in key %s to sessionStorage', (key) => {
    storage.setItem(key, 'a-value');
    expect(sessionStorage.getItem(key)).toBe('a-value');
  });

  it('routes an unknown key (like access_token) to memory, not to sessionStorage or localStorage', () => {
    storage.setItem('access_token', 'a-token');
    expect(storage.getItem('access_token')).toBe('a-token');
    expect(sessionStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('access_token')).toBeNull();
  });
});
