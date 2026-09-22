import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';

describe('App', () => {
  function configure(auth: Partial<AuthService>): void {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  }

  it('should create the app', () => {
    configure({ userName: () => null });
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows the user name and a logout button when logged in', () => {
    configure({ userName: () => 'Sam Sales' });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sam Sales');
    expect(compiled.querySelector('button')?.textContent).toContain('Logout');
  });

  it('shows no user name and no logout button when not logged in', () => {
    configure({ userName: () => null });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('button')).toBeNull();
  });

  it('calls AuthService.logout() when the logout button is clicked', () => {
    const logout = vi.fn();
    configure({ userName: () => 'Sam Sales', logout });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector('button')?.dispatchEvent(new Event('click'));
    expect(logout).toHaveBeenCalledTimes(1);
  });
});
