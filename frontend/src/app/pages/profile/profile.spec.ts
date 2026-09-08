import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { UserDataExport, UserResponse } from '../../core/models/auth.models';
import { UserService } from '../../core/users/user.service';
import { Profile } from './profile';

describe('Profile', () => {
  let userServiceStub: {
    getCurrentUser: ReturnType<typeof vi.fn>;
    exportUserData: ReturnType<typeof vi.fn>;
    anonymizeUser: ReturnType<typeof vi.fn>;
  };
  let authServiceStub: { logout: ReturnType<typeof vi.fn> };
  let router: Router;
  const user: UserResponse = {
    id: 1,
    email: 'jean.dupont@techcorp.com',
    nom: 'Dupont',
    prenom: 'Jean',
    role: 'EMPLOYEE',
    companyId: 2,
    companyName: 'TechCorp',
  };

  beforeEach(() => {
    userServiceStub = {
      getCurrentUser: vi.fn(() => of(user)),
      exportUserData: vi.fn(),
      anonymizeUser: vi.fn(),
    };
    authServiceStub = { logout: vi.fn() };

    TestBed.configureTestingModule({
      imports: [Profile],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceStub },
        { provide: AuthService, useValue: authServiceStub },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.spyOn(window, 'confirm');
  });

  it('loads and displays the current user on init', () => {
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Jean Dupont');
    expect(compiled.textContent).toContain('TechCorp');
  });

  it('shows an error message when loading the profile fails', () => {
    userServiceStub.getCurrentUser.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Une erreur inattendue est survenue');
  });

  it('exportData downloads a JSON file built from the export payload', () => {
    const exportPayload: UserDataExport = {
      user,
      bookings: [],
      exportDate: '2030-01-15T00:00:00.000Z',
    };
    userServiceStub.exportUserData.mockReturnValue(of(exportPayload));
    const createObjectURL = vi.fn(() => 'blob:mock-url');
    const revokeObjectURL = vi.fn();
    vi.spyOn(URL, 'createObjectURL').mockImplementation(createObjectURL);
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(revokeObjectURL);
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    (fixture.componentInstance as unknown as { exportData(): void }).exportData();

    expect(userServiceStub.exportUserData).toHaveBeenCalled();
    expect(createObjectURL).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');

    vi.restoreAllMocks();
  });

  it('does nothing when the anonymization confirmation is declined', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { anonymize(): void }).anonymize();

    expect(userServiceStub.anonymizeUser).not.toHaveBeenCalled();
  });

  it('anonymizes, logs out and redirects to /connexion when confirmed', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    userServiceStub.anonymizeUser.mockReturnValue(of(undefined));
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { anonymize(): void }).anonymize();

    expect(userServiceStub.anonymizeUser).toHaveBeenCalled();
    expect(authServiceStub.logout).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/connexion');
  });
});
