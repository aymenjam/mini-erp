import { Injectable } from '@angular/core';
import { MemoryStorage, OAuthStorage } from 'angular-oauth2-oidc';

/**
 * Keys the library writes only during the sign-in redirect: the PKCE verifier, the nonce and the
 * return path (`requested_route`). They must survive the round trip to Keycloak, so they go to
 * sessionStorage (per tab, gone when the tab closes). Checked in the library source (22.0.2).
 */
const SIGN_IN_KEYS = new Set(['nonce', 'PKCE_verifier', 'requested_route']);

/**
 * AC-11: no token in localStorage. angular-oauth2-oidc has one OAuthStorage for everything
 * (tokens and sign-in state alike), so we route by key: the three sign-in keys above go to
 * sessionStorage, every other key (access_token, id_token, refresh_token, ...) defaults to
 * memory. An unknown key is treated as a token and kept in memory: a library upgrade that adds a
 * new sign-in key would break login (caught by a test), never leak a token to storage.
 */
@Injectable({ providedIn: 'root' })
export class SignInStateStorage implements OAuthStorage {
  private readonly memory = new MemoryStorage();

  getItem(key: string): string | null {
    return this.store(key).getItem(key);
  }

  removeItem(key: string): void {
    this.store(key).removeItem(key);
  }

  setItem(key: string, data: string): void {
    this.store(key).setItem(key, data);
  }

  private store(key: string): OAuthStorage {
    return SIGN_IN_KEYS.has(key) ? sessionStorage : this.memory;
  }
}
