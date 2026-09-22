import { Routes } from '@angular/router';
import { AuthCallback } from './core/auth/auth-callback';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'customers' },
  { path: 'auth/callback', component: AuthCallback },
  {
    path: 'customers',
    canActivate: [authGuard],
    loadComponent: () => import('./features/customers/customer-list').then((m) => m.CustomerList),
  },
];
