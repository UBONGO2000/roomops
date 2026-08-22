import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../core/auth/auth.service';
import { describeApiError } from '../../core/http/error-message';
import { UserResponse } from '../../core/models/auth.models';
import { UserService } from '../../core/users/user.service';

@Component({
  selector: 'app-profile',
  imports: [MatButtonModule, MatCardModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  protected readonly user = signal<UserResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly exporting = signal(false);
  protected readonly anonymizing = signal(false);

  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.loading.set(true);
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.user.set(user);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }

  protected exportData(): void {
    this.errorMessage.set(null);
    this.exporting.set(true);
    this.userService.exportUserData().subscribe({
      next: (data) => {
        this.exporting.set(false);
        this.downloadJson(data, `roomops-export-${data.user.email}.json`);
      },
      error: (error: unknown) => {
        this.exporting.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }

  protected anonymize(): void {
    const confirmed = window.confirm(
      'Anonymiser votre compte remplace définitivement votre nom, prénom et email par des ' +
        'valeurs anonymes. Cette action est IRRÉVERSIBLE et vous déconnecte immédiatement. ' +
        'Continuer ?',
    );
    if (!confirmed) {
      return;
    }

    this.errorMessage.set(null);
    this.anonymizing.set(true);
    this.userService.anonymizeUser().subscribe({
      next: () => {
        this.authService.logout();
        this.router.navigateByUrl('/connexion');
      },
      error: (error: unknown) => {
        this.anonymizing.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }

  private downloadJson(data: unknown, filename: string): void {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
