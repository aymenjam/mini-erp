import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from './auth.service';
import { APP_CONFIG } from '../config/app-config';

/**
 * AC-11 / scope item 8: the access token is sent only to the gateway origin. Origin comparison
 * (not startsWith on the raw URL string), so a URL where the gateway's address is only userinfo
 * (http://localhost:8090@evil.test/...) gets nothing: its real origin is http://evil.test.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const config = inject(APP_CONFIG);

  const gatewayOrigin = new URL(config.gatewayBaseUrl).origin;
  const requestOrigin = new URL(req.url, document.baseURI).origin;
  const token = requestOrigin === gatewayOrigin ? auth.getAccessToken() : null;

  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
