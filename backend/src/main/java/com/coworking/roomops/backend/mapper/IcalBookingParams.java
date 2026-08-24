package com.coworking.roomops.backend.mapper;

import java.time.LocalDateTime;

/** Paramètres de création de réservation extraits d'un VEVENT importé (cf. IcalMapper). */
public record IcalBookingParams(Long roomId, LocalDateTime dateDebut, LocalDateTime dateFin, String motif) {}
