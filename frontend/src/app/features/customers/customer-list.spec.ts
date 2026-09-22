import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { CustomerList } from './customer-list';
import { CustomersService } from './customers.service';
import { AuthService } from '../../core/auth/auth.service';
import { Page } from '../../core/api/page';
import { Customer } from './customer';

// AC-7: the empty message; an error message on 401; an error message on 403; rows shown when
// content is not empty. Uses a fake CustomersService.
describe('CustomerList', () => {
  function render(list: () => Observable<Page<Customer>>, login = vi.fn()) {
    TestBed.configureTestingModule({
      providers: [
        { provide: CustomersService, useValue: { list } },
        { provide: AuthService, useValue: { login } },
      ],
    });
    const fixture = TestBed.createComponent(CustomerList);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement, login };
  }

  it('shows "No customers yet" when the page is empty', () => {
    const page: Page<Customer> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    const { element } = render(() => of(page));
    expect(element.textContent).toContain('No customers yet');
  });

  it('shows a row for each customer when content is not empty', () => {
    const page: Page<Customer> = {
      content: [{ id: 'a' }, { id: 'b' }],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    };
    const { element } = render(() => of(page));
    expect(element.querySelectorAll('tbody tr').length).toBe(2);
    expect(element.textContent).toContain('a');
    expect(element.textContent).toContain('b');
  });

  it('shows a session-ended message and a sign-in button on 401', () => {
    const { element, login } = render(() => throwError(() => new HttpErrorResponse({ status: 401 })));
    const alert = element.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('session has ended');
    const button = element.querySelector('button');
    expect(button?.textContent).toContain('Sign in');
    button?.dispatchEvent(new Event('click'));
    expect(login).toHaveBeenCalledTimes(1);
  });

  it('shows a role message and no sign-in button on 403', () => {
    const { element } = render(() => throwError(() => new HttpErrorResponse({ status: 403 })));
    const alert = element.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('SALES or ADMIN');
    expect(element.querySelector('button')).toBeNull();
  });

  it('shows a generic message on any other error, without the server body', () => {
    const { element } = render(() =>
      throwError(() => new HttpErrorResponse({ status: 500, error: 'super secret stack trace' })),
    );
    const alert = element.querySelector('[role="alert"]');
    expect(alert?.textContent).not.toContain('super secret stack trace');
    expect(alert?.textContent).toContain('Something went wrong');
  });
});
