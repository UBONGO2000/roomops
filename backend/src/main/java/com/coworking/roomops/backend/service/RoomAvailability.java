package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Room;

public record RoomAvailability(Room room, boolean available, String reason) {}
