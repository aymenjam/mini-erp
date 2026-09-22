import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../core/config/app-config';
import { Page } from '../../core/api/page';
import { Customer } from './customer';

@Injectable({ providedIn: 'root' })
export class CustomersService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  /** GET /api/v1/customers through the gateway. */
  list(): Observable<Page<Customer>> {
    return this.http.get<Page<Customer>>(`${this.config.gatewayBaseUrl}/api/v1/customers`);
  }
}
