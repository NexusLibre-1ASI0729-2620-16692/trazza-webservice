package trazzaplatform.api.iam.application.internal.outboundservices;

/// <summary>Password hashing (BCrypt). The password itself is never stored.</summary>
public interface IHashingService {

    String hashPassword(String password);

    boolean verifyPassword(String password, String passwordHash);
}