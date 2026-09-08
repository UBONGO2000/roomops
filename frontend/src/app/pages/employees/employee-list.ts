import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CompanyService } from '../../core/companies/company.service';
import { describeApiError } from '../../core/http/error-message';
import { UserResponse } from '../../core/models/auth.models';
import { CompanyResponse } from '../../core/models/company.models';
import { UserService } from '../../core/users/user.service';

@Component({
  selector: 'app-employee-list',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList implements OnInit {
  private readonly formBuilder = new FormBuilder();

  protected readonly companyForm = this.formBuilder.nonNullable.group({
    nom: ['', Validators.required],
    siret: [''],
    adresseFacturation: [''],
    tarifHoraire: this.formBuilder.control<number | null>(null),
  });

  protected readonly employeeForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    role: this.formBuilder.nonNullable.control<'EMPLOYEE' | 'MANAGER'>('EMPLOYEE'),
  });

  protected readonly currentUser = signal<UserResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly companies = signal<CompanyResponse[]>([]);
  protected readonly selectedCompanyId = signal<number | null>(null);
  protected readonly creatingCompany = signal(false);
  protected readonly deletingCompany = signal(false);

  protected readonly employees = signal<UserResponse[]>([]);
  protected readonly loadingEmployees = signal(false);
  protected readonly addingEmployee = signal(false);
  protected readonly removingId = signal<number | null>(null);

  protected readonly isSuperAdmin = computed(() => this.currentUser()?.role === 'SUPER_ADMIN');
  private readonly isManager = computed(() => this.currentUser()?.role === 'MANAGER');

  // Un Manager gère toujours sa propre entreprise ; un Super-Admin doit d'abord en sélectionner
  // une (aucune notion de companyId "courante" pour ce rôle, cf. chk_user_company_by_role).
  protected readonly targetCompanyId = computed(() =>
    this.isManager() ? (this.currentUser()?.companyId ?? null) : this.selectedCompanyId(),
  );

  constructor(
    private readonly userService: UserService,
    private readonly companyService: CompanyService,
  ) {}

  ngOnInit(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.loading.set(false);

        if (user.role === 'SUPER_ADMIN') {
          this.loadCompanies();
        } else if (user.role === 'MANAGER') {
          if (!user.companyId) {
            this.errorMessage.set('Utilisateur sans entreprise rattachée.');
            return;
          }
          this.loadEmployees(user.companyId);
        }
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }

  protected selectCompany(companyId: number): void {
    this.selectedCompanyId.set(companyId);
    this.loadEmployees(companyId);
  }

  protected deleteCompany(): void {
    const companyId = this.selectedCompanyId();
    const company = this.companies().find((item) => item.id === companyId);
    if (!company || !window.confirm(`Supprimer l'entreprise ${company.nom} ?`)) {
      return;
    }

    this.deletingCompany.set(true);
    this.errorMessage.set(null);
    this.companyService.deleteCompany(company.id).subscribe({
      next: () => {
        this.companies.update((list) => list.filter((item) => item.id !== company.id));
        this.selectedCompanyId.set(null);
        this.employees.set([]);
        this.deletingCompany.set(false);
      },
      error: (error: unknown) => {
        this.deletingCompany.set(false);
        this.errorMessage.set(
          describeApiError(error, {
            409: 'Impossible de supprimer cette entreprise : elle possède des réservations.',
            403: "Seul un Super-Admin peut supprimer une entreprise.",
          }),
        );
      },
    });
  }

  protected createCompany(): void {
    if (this.companyForm.invalid) {
      this.companyForm.markAllAsTouched();
      return;
    }

    const { nom, siret, adresseFacturation, tarifHoraire } = this.companyForm.getRawValue();
    this.creatingCompany.set(true);
    this.errorMessage.set(null);

    this.companyService
      .createCompany({
        nom,
        siret: siret || undefined,
        adresseFacturation: adresseFacturation || undefined,
        tarifHoraire: tarifHoraire ?? undefined,
      })
      .subscribe({
        next: (created) => {
          this.creatingCompany.set(false);
          this.companies.update((list) => [...list, created]);
          this.companyForm.reset();
          this.selectCompany(created.id);
        },
        error: (error: unknown) => {
          this.creatingCompany.set(false);
          this.errorMessage.set(describeApiError(error));
        },
      });
  }

  protected addEmployee(): void {
    const companyId = this.targetCompanyId();
    if (!companyId || this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      return;
    }

    this.addingEmployee.set(true);
    this.errorMessage.set(null);

    this.companyService.addEmployee(companyId, this.employeeForm.getRawValue()).subscribe({
      next: (created) => {
        this.addingEmployee.set(false);
        this.employees.update((list) => [...list, created]);
        this.employeeForm.reset({ role: 'EMPLOYEE' });
      },
      error: (error: unknown) => {
        this.addingEmployee.set(false);
        this.errorMessage.set(
          describeApiError(error, {
            403: "Vous n'êtes pas autorisé à ajouter un employé à cette entreprise.",
          }),
        );
      },
    });
  }

  protected removeEmployee(employee: UserResponse): void {
    const companyId = this.targetCompanyId();
    if (!companyId) {
      return;
    }

    const confirmed = window.confirm(
      `Retirer ${employee.prenom} ${employee.nom} de cette entreprise ?`,
    );
    if (!confirmed) {
      return;
    }

    this.removingId.set(employee.id);
    this.errorMessage.set(null);

    this.companyService.removeEmployee(companyId, employee.id).subscribe({
      next: () => {
        this.removingId.set(null);
        this.employees.update((list) => list.filter((item) => item.id !== employee.id));
      },
      error: (error: unknown) => {
        this.removingId.set(null);
        this.errorMessage.set(
          describeApiError(error, {
            409: 'Impossible de supprimer cet employé : il a des réservations associées.',
            403: "Vous n'êtes pas autorisé à retirer cet employé.",
          }),
        );
      },
    });
  }

  private loadCompanies(): void {
    this.companyService.listCompanies().subscribe({
      next: (companies) => this.companies.set(companies),
      error: (error: unknown) => this.errorMessage.set(describeApiError(error)),
    });
  }

  private loadEmployees(companyId: number): void {
    this.loadingEmployees.set(true);
    this.companyService.getCompanyEmployees(companyId).subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.loadingEmployees.set(false);
      },
      error: (error: unknown) => {
        this.loadingEmployees.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }
}
