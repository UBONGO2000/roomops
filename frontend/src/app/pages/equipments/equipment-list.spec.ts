import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { EquipmentService } from '../../core/equipment/equipment.service';
import {
  EquipmentResponse,
  EquipmentStatusUpdateResponse,
} from '../../core/models/equipment.models';
import { EquipmentList } from './equipment-list';

describe('EquipmentList', () => {
  let equipmentServiceStub: {
    listEquipments: ReturnType<typeof vi.fn>;
    updateStatus: ReturnType<typeof vi.fn>;
  };
  const equipments: EquipmentResponse[] = [
    { id: 1, type: 'Projecteur', roomId: 1, roomName: 'Salle Alpha', statut: 'OPERATIONNEL' },
  ];

  beforeEach(() => {
    equipmentServiceStub = {
      listEquipments: vi.fn(() => of(equipments)),
      updateStatus: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [EquipmentList],
      providers: [{ provide: EquipmentService, useValue: equipmentServiceStub }],
    });
  });

  it('loads and displays the equipment list on init', () => {
    const fixture = TestBed.createComponent(EquipmentList);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Projecteur');
    expect(compiled.textContent).toContain('Salle Alpha');
  });

  it('shows an error message when loading equipment fails', () => {
    equipmentServiceStub.listEquipments.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    const fixture = TestBed.createComponent(EquipmentList);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Une erreur inattendue est survenue');
  });

  it('does nothing when the confirmation to change status is declined', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = TestBed.createComponent(EquipmentList);
    fixture.detectChanges();

    (
      fixture.componentInstance as unknown as { toggleStatus(equipment: EquipmentResponse): void }
    ).toggleStatus(equipments[0]);

    expect(equipmentServiceStub.updateStatus).not.toHaveBeenCalled();
  });

  it('declares a panne and reports how many bookings were cancelled', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const response: EquipmentStatusUpdateResponse = {
      id: 1,
      type: 'Projecteur',
      roomId: 1,
      roomName: 'Salle Alpha',
      statut: 'EN_PANNE',
      reservationsAnnulees: 2,
    };
    equipmentServiceStub.updateStatus.mockReturnValue(of(response));

    const fixture = TestBed.createComponent(EquipmentList);
    fixture.detectChanges();
    (
      fixture.componentInstance as unknown as { toggleStatus(equipment: EquipmentResponse): void }
    ).toggleStatus(equipments[0]);
    fixture.detectChanges();

    expect(equipmentServiceStub.updateStatus).toHaveBeenCalledWith(1, 'EN_PANNE');
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('2 réservation(s) annulée(s)');
  });

  it('reports a dedicated message when returning an equipment to service fails with 403', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    equipmentServiceStub.updateStatus.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );

    const fixture = TestBed.createComponent(EquipmentList);
    fixture.detectChanges();
    (
      fixture.componentInstance as unknown as { toggleStatus(equipment: EquipmentResponse): void }
    ).toggleStatus(equipments[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain(
      "Vous n'êtes pas autorisé à modifier le statut des équipements.",
    );
  });
});
