package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.UserAccount;
import com.example.hexhiveint.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserAccountRepository repository;

    @PostMapping("/login")
    public boolean login(@RequestBody UserAccount credentials) {
        Optional<UserAccount> user = repository.findById(credentials.getUsername());
        return user.map(u -> u.getPassword().equals(credentials.getPassword())).orElse(false);
    }
}
