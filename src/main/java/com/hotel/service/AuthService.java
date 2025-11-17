package com.hotel.service;

import com.hotel.model.User;
import com.hotel.repository.HibernateUserRepository;

import java.util.Optional;

public class AuthService {
    private final HibernateUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(HibernateUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> user.isActive())
                .filter(user -> user.getPasswordHash().equals(passwordEncoder.encode(password)));
    }
}
