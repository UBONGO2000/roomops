import { Component } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatButtonModule, MatToolbarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly currentUser;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.currentUser = this.authService.currentUser;
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }
}
