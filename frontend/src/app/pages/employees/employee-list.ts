import { Component, OnInit, signal } from '@angular/core';
import { switchMap } from 'rxjs';
import { CompanyService } from '../../core/companies/company.service';
import { describeApiError } from '../../core/http/error-message';
import { UserResponse } from '../../core/models/auth.models';
import { UserService } from '../../core/users/user.service';

@Component({
  selector: 'app-employee-list',
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList implements OnInit {
  protected readonly employees = signal<UserResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor(
    private readonly userService: UserService,
    private readonly companyService: CompanyService,
  ) {}

  ngOnInit(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    // companyId n'est pas encodé dans le JWT (cf. jwt.util.ts) : il faut d'abord résoudre
    // l'utilisateur courant pour connaître l'entreprise dont on doit lister les employés.
    this.userService
      .getCurrentUser()
      .pipe(
        switchMap((currentUser) => {
          if (!currentUser.companyId) {
            throw new Error('Utilisateur sans entreprise rattachée');
          }
          return this.companyService.getCompanyEmployees(currentUser.companyId);
        }),
      )
      .subscribe({
        next: (employees) => {
          this.employees.set(employees);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.errorMessage.set(describeApiError(error));
        },
      });
  }
}
