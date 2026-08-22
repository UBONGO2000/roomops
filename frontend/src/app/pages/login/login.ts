import { Component, ElementRef, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth/auth.service';
import { describeApiError } from '../../core/http/error-message';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly formBuilder = new FormBuilder();

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly passwordVisible = signal(false);
  protected readonly loginError = signal<string | null>(null);

  protected readonly demoCredentials = {
    email: 'admin@roomops.local',
    password: 'ChangeMe123!',
  };

  private readonly emailInput = viewChild<ElementRef<HTMLInputElement>>('emailInput');
  private readonly passwordInput = viewChild<ElementRef<HTMLInputElement>>('passwordInput');

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl('/reservations');
    }
  }

  protected togglePasswordVisibility(): void {
    this.passwordVisible.set(!this.passwordVisible());
  }

  protected onSubmit(): void {
    this.submitted.set(true);
    this.loginError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.controls.email.invalid) {
        this.emailInput()?.nativeElement.focus();
      } else if (this.form.controls.password.invalid) {
        this.passwordInput()?.nativeElement.focus();
      }
      return;
    }

    this.submitting.set(true);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        const redirect =
          this.activatedRoute.snapshot.queryParamMap.get('redirect') ?? '/reservations';
        this.router.navigateByUrl(redirect);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.loginError.set(describeApiError(error, { 401: 'Email ou mot de passe incorrect.' }));
      },
    });
  }
}
