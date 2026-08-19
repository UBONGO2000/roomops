import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

interface KeyFigure {
  value: string;
  label: string;
}

type FeatureIcon = 'calendar' | 'alert' | 'shield' | 'people';

interface Feature {
  icon: FeatureIcon;
  title: string;
  description: string;
}

interface ComparisonPoint {
  before: string;
  after: string;
}

@Component({
  selector: 'app-home',
  imports: [MatCardModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  protected readonly keyFigures: KeyFigure[] = [
    { value: '18', label: 'salles de réunion' },
    { value: '2', label: 'bâtiments' },
    { value: '50', label: 'entreprises hébergées' },
    { value: '1000', label: 'collaborateurs au quotidien' },
  ];

  protected readonly comparisonPoints: ComparisonPoint[] = [
    {
      before: 'Un tableur partagé mis à jour à la main, avec toujours un onglet en retard.',
      after: 'Un seul planning, à jour en permanence, consultable par tous.',
    },
    {
      before:
        "Deux entreprises persuadées d'avoir la salle Alpha à 14h, découvert seulement sur place.",
      after:
        'Un créneau ne peut être pris deux fois : la garantie est posée au niveau de la base de données, pas juste vérifiée à l’écran.',
    },
    {
      before: "Un vidéoprojecteur en panne depuis trois jours, que personne n'a signalé.",
      after:
        'Une salle en panne devient indisponible immédiatement, réservations concernées annulées.',
    },
  ];

  protected readonly features: Feature[] = [
    {
      icon: 'calendar',
      title: 'Zéro double réservation',
      description:
        'Chaque créneau est vérifié à la réservation, avec une garantie appliquée au niveau de la base de données : deux réservations ne peuvent jamais se chevaucher sur une même salle.',
    },
    {
      icon: 'alert',
      title: 'Suivi des pannes en temps réel',
      description:
        "Dès qu'un équipement est déclaré en panne, la salle concernée devient indisponible et les réservations futures concernées sont automatiquement annulées.",
    },
    {
      icon: 'shield',
      title: 'Conforme RGPD',
      description:
        'Export des données personnelles et droit à l’oubli intégrés : les comptes anonymisés voient aussi leurs sessions actives révoquées immédiatement.',
    },
    {
      icon: 'people',
      title: 'Des rôles adaptés à votre organisation',
      description:
        'Employé, Manager ou Super-Admin : chacun accède uniquement aux réservations et aux actions qui le concernent.',
    },
  ];
}
