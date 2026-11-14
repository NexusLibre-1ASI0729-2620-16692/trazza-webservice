package com.trazza.iam.infrastructure.hashing;

import com.trazza.iam.application.internal.outboundservices.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de hashing usando BCrypt (Spring Security Crypto).
 */
@Service
public class BCryptHashingService implements HashingService {

    private final PasswordEncoder passwordEncoder;

    public BCryptHashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean verifyPassword(String password, String passwordHash) {
        return passwordEncoder.matches(password, passwordHash);
    }
}