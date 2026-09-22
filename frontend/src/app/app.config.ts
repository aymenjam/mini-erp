import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { OAuthStorage, provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { APP_CONFIG } from './core/config/app-config';
import { environment } from '../environments/environment';
import { authInterceptor } from './core/auth/auth.interceptor';
import { SignInStateStorage } from './core/auth/sign-in-state-storage';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: APP_CONFIG, useValue: environment },
    provideOAuthClient(),
    // Overrides provideOAuthClient's default (sessionStorage for everything): AC-11 storage split.
    { provide: OAuthStorage, useClass: SignInStateStorage },
    // Configures the library and processes an OIDC callback before the app renders a route.
    provideAppInitializer(() => inject(AuthService).initialize()),
  ],
};
