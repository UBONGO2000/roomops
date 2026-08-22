import { Component, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { EquipmentService } from '../../core/equipment/equipment.service';
import { describeApiError } from '../../core/http/error-message';
import { EquipmentResponse } from '../../core/models/equipment.models';

@Component({
  selector: 'app-equipment-list',
  imports: [MatButtonModule],
  templateUrl: './equipment-list.html',
  styleUrl: './equipment-list.scss',
})
export class EquipmentList implements OnInit {
  protected readonly equipments = signal<EquipmentResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly infoMessage = signal<string | null>(null);
  protected readonly togglingId = signal<number | null>(null);

  constructor(private readonly equipmentService: EquipmentService) {}

  ngOnInit(): void {
    this.loadEquipments();
  }

  protected toggleStatus(equipment: EquipmentResponse): void {
    const declaringPanne = equipment.statut === 'OPERATIONNEL';
    const nextStatut = declaringPanne ? 'EN_PANNE' : 'OPERATIONNEL';

    const confirmationMessage = declaringPanne
      ? `Déclarer « ${equipment.type} » (${equipment.roomName}) en panne annulera automatiquement ` +
        'toutes les réservations futures confirmées de cette salle. Continuer ?'
      : `Remettre « ${equipment.type} » (${equipment.roomName}) en service ?`;

    if (!window.confirm(confirmationMessage)) {
      return;
    }

    this.errorMessage.set(null);
    this.infoMessage.set(null);
    this.togglingId.set(equipment.id);

    this.equipmentService.updateStatus(equipment.id, nextStatut).subscribe({
      next: (updated) => {
        this.togglingId.set(null);
        this.equipments.update((list) =>
          list.map((item) => (item.id === updated.id ? { ...item, statut: updated.statut } : item)),
        );
        if (declaringPanne) {
          this.infoMessage.set(
            updated.reservationsAnnulees > 0
              ? `${updated.reservationsAnnulees} réservation(s) annulée(s) suite à cette panne.`
              : 'Aucune réservation future à annuler pour cette salle.',
          );
        }
      },
      error: (error: unknown) => {
        this.togglingId.set(null);
        this.errorMessage.set(
          describeApiError(error, {
            403: "Vous n'êtes pas autorisé à modifier le statut des équipements.",
          }),
        );
      },
    });
  }

  private loadEquipments(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.equipmentService.listEquipments().subscribe({
      next: (equipments) => {
        this.equipments.set(equipments);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }
}
