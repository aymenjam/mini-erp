import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { APP_CONFIG } from '../config/app-config';

// Scope item 8 / AC-11: the access token is added only for requests to the gateway origin.
describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getAccessToken: () => 'the-token' } },
        { provide: APP_CONFIG, useValue: { gatewayBaseUrl: 'http://localhost:8090', oidc: { authority: '', clientId: '' } } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('adds the token for a request to the gateway origin', () => {
    http.get('http://localhost:8090/api/v1/customers').subscribe();
    const req = httpTesting.expectOne('http://localhost:8090/api/v1/customers');
    expect(req.request.headers.get('Authorization')).toBe('Bearer the-token');
  });

  it('does not add the token for another origin', () => {
    http.get('https://other.example.test/api/v1/customers').subscribe();
    const req = httpTesting.expectOne('https://other.example.test/api/v1/customers');
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('does not add the token for a URL where "http://localhost:8090" is only userinfo, not the host (startsWith trap)', () => {
    // Valid URL syntax: user "localhost:8090" at host "evil.test" (URL.origin is http://evil.test).
    // A naive req.url.startsWith(gatewayBaseUrl) check would wrongly match this string.
    const trapUrl = 'http://localhost:8090@evil.test/api/v1/customers';
    http.get(trapUrl).subscribe();
    const req = httpTesting.expectOne(trapUrl);
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('does not add the token for a relative URL (not the gateway origin)', () => {
    http.get('/assets/config.json').subscribe();
    const req = httpTesting.expectOne('/assets/config.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
  });
});
