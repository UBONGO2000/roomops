package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.AuthApi;
import com.coworking.roomops.backend.model.LoginRequest;
import com.coworking.roomops.backend.model.RefreshToken200Response;
import com.coworking.roomops.backend.model.RefreshTokenRequest;
import com.coworking.roomops.backend.model.TokenResponse;
import com.coworking.roomops.backend.service.AuthService;
import com.coworking.roomops.backend.service.TokenPair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
        TokenPair tokens = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
        TokenResponse response =
                new TokenResponse().accessToken(tokens.accessToken()).refreshToken(tokens.refreshToken()).tokenType("Bearer");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<RefreshToken200Response> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String newAccessToken = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(new RefreshToken200Response().accessToken(newAccessToken).tokenType("Bearer"));
    }
}
