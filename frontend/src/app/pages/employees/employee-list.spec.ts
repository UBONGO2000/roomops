import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { UserResponse } from '../../core/models/auth.models';
import { CompanyResponse } from '../../core/models/company.models';
import { CompanyService } from '../../core/companies/company.service';
import { UserService } from '../../core/users/user.service';
import { EmployeeList } from './employee-list';

describe('EmployeeList', () => {
  let userServiceStub: { getCurrentUser: ReturnType<typeof vi.fn> };
  let companyServiceStub: {
    listCompanies: ReturnType<typeof vi.fn>;
    createCompany: ReturnType<typeof vi.fn>;
    deleteCompany: ReturnType<typeof vi.fn>;
    getCompanyEmployees: ReturnType<typeof vi.fn>;
    addEmployee: ReturnType<typeof vi.fn>;
    removeEmployee: ReturnType<typeof vi.fn>;
  };

  const manager: UserResponse = {
    id: 1,
    email: 'manager@techcorp.com',
    nom: 'Dubois',
    prenom: 'Claire',
    role: 'MANAGER',
    companyId: 2,
    companyName: 'TechCorp',
  };
  const superAdmin: UserResponse = {
    id: 2,
    email: 'admin@roomops.local',
    nom: 'Admin',
    prenom: 'RoomOps',
    role: 'SUPER_ADMIN',
    companyId: null,
  };
  const employee: UserResponse = {
    id: 3,
    email: 'employe@techcorp.com',
    nom: 'Haddad',
    prenom: 'Karim',
    role: 'EMPLOYEE',
    companyId: 2,
  };

  type ComponentInternals = {
    employeeForm: {
      getRawValue(): unknown;
      setValue(value: unknown): void;
      controls: Record<string, unknown>;
    };
    companyForm: { setValue(value: unknown): void };
    createCompany(): void;
    deleteCompany(): void;
    addEmployee(): void;
    removeEmployee(target: UserResponse): void;
    selectCompany(companyId: number): void;
    employees: () => UserResponse[];
    companies: () => CompanyResponse[];
    errorMessage: () => string | null;
  };

  function internals(fixture: { componentInstance: unknown }): ComponentInternals {
    return fixture.componentInstance as unknown as ComponentInternals;
  }

  beforeEach(() => {
    userServiceStub = { getCurrentUser: vi.fn() };
    companyServiceStub = {
      listCompanies: vi.fn(() => of([])),
      createCompany: vi.fn(),
      deleteCompany: vi.fn(),
      getCompanyEmployees: vi.fn(() => of([])),
      addEmployee: vi.fn(),
      removeEmployee: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [EmployeeList],
      providers: [
        { provide: UserService, useValue: userServiceStub },
        { provide: CompanyService, useValue: companyServiceStub },
      ],
    });
  });

  describe('as a Manager', () => {
    beforeEach(() => userServiceStub.getCurrentUser.mockReturnValue(of(manager)));

    it("loads its own company's employees on init", () => {
      companyServiceStub.getCompanyEmployees.mockReturnValue(of([employee]));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      expect(companyServiceStub.getCompanyEmployees).toHaveBeenCalledWith(2);
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Karim Haddad');
    });

    it('shows an error and does not load employees when the manager has no company', () => {
      userServiceStub.getCurrentUser.mockReturnValue(of({ ...manager, companyId: null }));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      expect(companyServiceStub.getCompanyEmployees).not.toHaveBeenCalled();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Utilisateur sans entreprise rattachée.');
    });

    it('adds an employee to its own company and resets the form', () => {
      const created: UserResponse = {
        id: 4,
        email: 'nouveau@techcorp.com',
        nom: 'Petit',
        prenom: 'Léa',
        role: 'EMPLOYEE',
        companyId: 2,
      };
      companyServiceStub.addEmployee.mockReturnValue(of(created));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();
      const component = internals(fixture);
      component.employeeForm.setValue({
        email: 'nouveau@techcorp.com',
        password: 'ChangeMe123!',
        nom: 'Petit',
        prenom: 'Léa',
        role: 'EMPLOYEE',
      });

      component.addEmployee();

      expect(companyServiceStub.addEmployee).toHaveBeenCalledWith(
        2,
        expect.objectContaining({ email: 'nouveau@techcorp.com' }),
      );
      expect(component.employees()).toContainEqual(created);
    });

    it('does not add an employee when the form is invalid', () => {
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      internals(fixture).addEmployee();

      expect(companyServiceStub.addEmployee).not.toHaveBeenCalled();
    });

    it('does nothing when the removal confirmation is declined', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(false);
      companyServiceStub.getCompanyEmployees.mockReturnValue(of([employee]));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      internals(fixture).removeEmployee(employee);

      expect(companyServiceStub.removeEmployee).not.toHaveBeenCalled();
    });

    it('removes an employee from the list when the removal is confirmed', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      companyServiceStub.getCompanyEmployees.mockReturnValue(of([employee]));
      companyServiceStub.removeEmployee.mockReturnValue(of(undefined));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();
      const component = internals(fixture);

      component.removeEmployee(employee);

      expect(companyServiceStub.removeEmployee).toHaveBeenCalledWith(2, employee.id);
      expect(component.employees()).not.toContainEqual(employee);
    });

    it('reports a dedicated message when removal fails because the employee has bookings', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      companyServiceStub.getCompanyEmployees.mockReturnValue(of([employee]));
      companyServiceStub.removeEmployee.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 409 })),
      );
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      internals(fixture).removeEmployee(employee);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('il a des réservations associées');
    });
  });

  describe('as a Super-Admin', () => {
    beforeEach(() => userServiceStub.getCurrentUser.mockReturnValue(of(superAdmin)));

    it('loads the company list instead of an employee list on init', () => {
      const companies: CompanyResponse[] = [{ id: 2, nom: 'TechCorp' }];
      companyServiceStub.listCompanies.mockReturnValue(of(companies));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      expect(companyServiceStub.listCompanies).toHaveBeenCalled();
      expect(companyServiceStub.getCompanyEmployees).not.toHaveBeenCalled();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Sélectionnez une entreprise');
    });

    it('loads the employees of the company selected from the dropdown', () => {
      companyServiceStub.getCompanyEmployees.mockReturnValue(of([employee]));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();

      internals(fixture).selectCompany(2);

      expect(companyServiceStub.getCompanyEmployees).toHaveBeenCalledWith(2);
    });

    it('creates a company and automatically selects it', () => {
      const created: CompanyResponse = { id: 5, nom: 'Nova Digital Studio' };
      companyServiceStub.createCompany.mockReturnValue(of(created));
      companyServiceStub.getCompanyEmployees.mockReturnValue(of([]));
      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();
      const component = internals(fixture);
      component.companyForm.setValue({
        nom: 'Nova Digital Studio',
        siret: '',
        adresseFacturation: '',
        tarifHoraire: null,
      });

      component.createCompany();

      expect(companyServiceStub.createCompany).toHaveBeenCalledWith(
        expect.objectContaining({ nom: 'Nova Digital Studio' }),
      );
      expect(component.companies()).toContainEqual(created);
      expect(companyServiceStub.getCompanyEmployees).toHaveBeenCalledWith(5);
    });

    it('removes a deleted company from the dropdown', () => {
      const company: CompanyResponse = { id: 5, nom: 'Entreprise à supprimer' };
      companyServiceStub.listCompanies.mockReturnValue(of([company]));
      companyServiceStub.deleteCompany.mockReturnValue(of(undefined));
      vi.spyOn(window, 'confirm').mockReturnValue(true);

      const fixture = TestBed.createComponent(EmployeeList);
      fixture.detectChanges();
      const component = internals(fixture);
      component.selectCompany(company.id);

      component.deleteCompany();

      expect(companyServiceStub.deleteCompany).toHaveBeenCalledWith(company.id);
      expect(component.companies()).not.toContainEqual(company);
    });
  });
});
