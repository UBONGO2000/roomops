package com.coworking.roomops.backend.security;

import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur authentifié introuvable"));
    }
}
