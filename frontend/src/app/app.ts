import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

/** Shell: user name and logout button (scope item 3), router outlet. */
@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly auth = inject(AuthService);

  protected userName(): string | null {
    return this.auth.userName();
  }

  protected logout(): void {
    this.auth.logout();
  }
}
