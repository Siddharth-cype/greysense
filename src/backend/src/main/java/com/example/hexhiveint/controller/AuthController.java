package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.UserAccount;
import com.example.hexhiveint.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for user authentication.
 *
 * <p>Provides a simple credential-based login endpoint. In production,
 * this should be replaced with a proper Spring Security configuration
 * using JWT or OAuth2.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserAccountRepository repository;

    /**
     * Authenticates a user by matching username and password against the database.
     *
     * @param credentials the {@link UserAccount} containing username and password
     * @return {@code true} if credentials match, {@code false} otherwise
     */
    @PostMapping("/login")
    public boolean login(@RequestBody UserAccount credentials) {
        Optional<UserAccount> user = repository.findById(credentials.getUsername());
        return user.map(u -> u.getPassword().equals(credentials.getPassword())).orElse(false);
    }
}
