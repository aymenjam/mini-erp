import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CustomersService } from './customers.service';
import { APP_CONFIG } from '../../core/config/app-config';
import { Page } from '../../core/api/page';
import { Customer } from './customer';

describe('CustomersService', () => {
  let service: CustomersService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: APP_CONFIG,
          useValue: { gatewayBaseUrl: 'http://localhost:8090', oidc: { authority: '', clientId: '' } },
        },
      ],
    });
    service = TestBed.inject(CustomersService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('calls GET {gateway}/api/v1/customers', () => {
    const page: Page<Customer> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    let result: Page<Customer> | undefined;
    service.list().subscribe((p) => (result = p));

    const req = httpTesting.expectOne('http://localhost:8090/api/v1/customers');
    expect(req.request.method).toBe('GET');
    req.flush(page);

    expect(result).toEqual(page);
  });
});
