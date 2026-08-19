package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.UsersApi;
import com.coworking.roomops.backend.mapper.BookingMapper;
import com.coworking.roomops.backend.mapper.UserMapper;
import com.coworking.roomops.backend.model.UserDataExport;
import com.coworking.roomops.backend.model.UserResponse;
import com.coworking.roomops.backend.service.PersonalDataExport;
import com.coworking.roomops.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UsersApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(UserMapper.toResponse(userService.getCurrentUser()));
    }

    @Override
    public ResponseEntity<UserDataExport> exportUserData() {
        PersonalDataExport export = userService.exportCurrentUserData();
        UserDataExport body =
                new UserDataExport()
                        .user(UserMapper.toResponse(export.user()))
                        .bookings(export.bookings().stream().map(BookingMapper::toResponse).toList())
                        .exportDate(export.exportDate());
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Void> anonymizeUser() {
        userService.anonymizeCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
