import { Component, ElementRef, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

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
  protected readonly passwordVisible = signal(false);

  protected readonly demoCredentials = {
    email: 'admin@roomops.local',
    password: 'ChangeMe123!',
  };

  private readonly emailInput = viewChild<ElementRef<HTMLInputElement>>('emailInput');
  private readonly passwordInput = viewChild<ElementRef<HTMLInputElement>>('passwordInput');

  protected togglePasswordVisibility(): void {
    this.passwordVisible.set(!this.passwordVisible());
  }

  protected onSubmit(): void {
    this.submitted.set(true);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.controls.email.invalid) {
        this.emailInput()?.nativeElement.focus();
      } else if (this.form.controls.password.invalid) {
        this.passwordInput()?.nativeElement.focus();
      }
      return;
    }

    // L'appel réel à POST /auth/login (AuthService HTTP, stockage des tokens, redirection
    // vers le tableau de bord) est prévu dans le ticket feature/frontend-auth — volontairement
    // pas implémenté ici : cette page valide le formulaire côté client, rien de plus pour
    // l'instant.
  }
}
