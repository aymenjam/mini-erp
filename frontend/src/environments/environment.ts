import { AppConfig } from '../app/core/config/app-config';

/**
 * Build-time config (spec 0.4, scope item 3, "one config place"). Same values work in dev mode
 * and in container mode (slice 4), because the browser stays on localhost in both.
 */
export const environment: AppConfig = {
  gatewayBaseUrl: 'http://localhost:8090',
  oidc: {
    authority: 'http://localhost:8080/realms/mini-erp',
    clientId: 'erp-frontend',
  },
};
