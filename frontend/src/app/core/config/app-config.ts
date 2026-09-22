import { InjectionToken } from '@angular/core';

/** OIDC settings needed to reach Keycloak. */
export interface OidcConfig {
  /** Keycloak realm URL, for example http://localhost:8080/realms/mini-erp. */
  readonly authority: string;
  /** Public client id of the frontend in the realm. */
  readonly clientId: string;
}

/** The one config place of the app (scope item 3 of spec 0.4). */
export interface AppConfig {
  /** Base URL of the gateway. Only this origin gets the access token (AC-11 interceptor rule). */
  readonly gatewayBaseUrl: string;
  readonly oidc: OidcConfig;
}

/**
 * Injection token so tests can provide a fake AppConfig instead of the real environment file.
 * Build-time environment files are used for now; container mode (slice 4) may replace the source.
 */
export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
