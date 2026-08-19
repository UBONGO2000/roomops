package com.coworking.roomops.backend.mapper;

import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.model.UserResponse;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        UserResponse response =
                new UserResponse()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nom(user.getNom())
                        .prenom(user.getPrenom())
                        .role(user.getRole().name());
        if (user.getCompany() != null) {
            response.companyId(user.getCompany().getId());
            response.companyName(user.getCompany().getNom());
        }
        return response;
    }
}
