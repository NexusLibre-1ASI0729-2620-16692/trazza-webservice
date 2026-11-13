package trazzaplatform.api.iam.application.internal.outboundservices;

/// <summary>Application-level protection of personal data (AES-256-GCM and HMAC-SHA256).</summary>
public interface IPersonalDataProtector {

    byte[] encrypt(String plainText);

    String decrypt(byte[] cipher);

    byte[] hashDni(String dni);
}