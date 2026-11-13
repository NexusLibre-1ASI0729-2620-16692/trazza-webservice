package com.trazza.iam.application.internal.outboundservices;

/**
 * Password hashing (BCrypt). The password itself is never stored.
 */
public interface IHashingService {

    String hashPassword(String password);

    boolean verifyPassword(String password, String passwordHash);
}