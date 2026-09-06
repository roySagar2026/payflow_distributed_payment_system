package com.payflow.payflow.service;

import com.payflow.payflow.model.User;
import com.payflow.payflow.model.UserStatus;
import com.payflow.payflow.model.Wallet;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.repository.UserRepository;
import com.payflow.payflow.repository.WalletRepository;
import com.payflow.payflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        // Auto-create a wallet for the new user
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .build();
        walletRepository.save(wallet);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), accessToken, refreshToken);
    }
}