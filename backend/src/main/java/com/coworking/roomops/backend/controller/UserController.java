package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.UsersApi;
import com.coworking.roomops.backend.mapper.UserMapper;
import com.coworking.roomops.backend.model.UserResponse;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UsersApi {

    private final CurrentUserProvider currentUserProvider;

    public UserController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(UserMapper.toResponse(currentUserProvider.get()));
    }
}
