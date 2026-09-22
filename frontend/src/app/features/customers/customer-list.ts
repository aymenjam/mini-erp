import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { CustomersService } from './customers.service';
import { Customer } from './customer';

type ViewState = 'loading' | 'empty' | 'list' | 'error';

/** /customers screen (scope item 3): the list, or "No customers yet", or a clear error message. */
@Component({
  selector: 'app-customer-list',
  templateUrl: './customer-list.html',
})
export class CustomerList implements OnInit {
  private readonly customersService = inject(CustomersService);
  private readonly auth = inject(AuthService);

  protected readonly state = signal<ViewState>('loading');
  protected readonly customers = signal<readonly Customer[]>([]);
  protected readonly errorMessage = signal('');
  protected readonly showSignIn = signal(false);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.customersService.list().subscribe({
      next: (page) => {
        this.customers.set(page.content);
        this.state.set(page.content.length === 0 ? 'empty' : 'list');
      },
      error: (error: HttpErrorResponse) => this.onError(error),
    });
  }

  protected signIn(): void {
    this.auth.login();
  }

  private onError(error: HttpErrorResponse): void {
    this.state.set('error');
    // No internal detail from the server body: fixed, general text per status.
    if (error.status === 401) {
      this.errorMessage.set('Your session has ended. Please sign in again.');
      this.showSignIn.set(true);
    } else if (error.status === 403) {
      this.errorMessage.set('You need the role SALES or ADMIN to see customers.');
      this.showSignIn.set(false);
    } else {
      this.errorMessage.set('Something went wrong. Please try again later.');
      this.showSignIn.set(false);
    }
  }
}
