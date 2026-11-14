package com.trazza.trazzawebservice.iam.application.internal.outboundservices.hashing;

/** Password hashing (BCrypt). The password itself is never stored. */
public interface HashingService {

    String hashPassword(String password);

    boolean verifyPassword(String password, String passwordHash);
}
