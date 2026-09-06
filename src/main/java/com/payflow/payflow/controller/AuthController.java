package com.payflow.payflow.controller;

import com.payflow.payflow.audit.Audited;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @io.swagger.v3.oas.annotations.Operation(
            summary = "Register a new user",
            description = "Creates a user account and automatically provisions an empty wallet."
    )
    @Audited(action = "USER_REGISTER", resourceType = "User")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Audited(action = "USER_LOGIN", resourceType = "User")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}