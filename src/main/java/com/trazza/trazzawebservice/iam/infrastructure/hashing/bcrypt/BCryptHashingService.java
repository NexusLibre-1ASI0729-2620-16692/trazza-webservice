package com.trazza.trazzawebservice.iam.infrastructure.hashing.bcrypt;

import com.trazza.trazzawebservice.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** BCrypt with work factor 11, compatible with the hashes written by the .NET version (BCrypt.Net). */
@Service
public class BCryptHashingService implements HashingService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(11);

    @Override
    public String hashPassword(String password) {
        return encoder.encode(password);
    }

    @Override
    public boolean verifyPassword(String password, String passwordHash) {
        try {
            return encoder.matches(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
