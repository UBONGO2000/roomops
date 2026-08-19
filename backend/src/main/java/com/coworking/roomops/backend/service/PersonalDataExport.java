package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.User;
import java.time.OffsetDateTime;
import java.util.List;

public record PersonalDataExport(User user, List<Booking> bookings, OffsetDateTime exportDate) {}
