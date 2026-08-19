package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
