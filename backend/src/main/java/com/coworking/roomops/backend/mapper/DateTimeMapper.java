package com.coworking.roomops.backend.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateTimeMapper {

    private DateTimeMapper() {}

    // Les timestamps sont stockés en base sans fuseau (hibernate.jdbc.time_zone=UTC) :
    // on normalise systématiquement en UTC ici pour ne pas dépendre de l'offset envoyé
    // par le client (ex: +02:00 vs Z) ni du fuseau par défaut de la JVM.
    public static LocalDateTime toUtcLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
