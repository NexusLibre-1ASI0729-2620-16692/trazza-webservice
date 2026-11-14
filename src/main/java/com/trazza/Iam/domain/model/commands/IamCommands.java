package com.trazza.iam.domain.model.commands;

import com.trazza.iam.domain.model.valueobjects.UserRoleType;
import java.util.List;

public class IamCommands {

    public record MerchantData(String businessName, String ruc, String businessType) {}

    public record SignUpCommand(
            String email, String password, String fullName, String dni, String phone,
            List<UserRoleType> roles, MerchantData merchant
    ) {
        // Bloque de inicialización para hacer la lista inmutable
        public SignUpCommand {
            roles = List.copyOf(roles);
        }
    }

    public record SignInCommand(String email, String password) {}

    public record RefreshSessionCommand(String refreshToken) {}

    public record SignOutCommand(Long userId, String refreshToken) {}

    public record UpdateIdentityCommand(Long userId, String fullName, String phone) {}

    public record AddRoleCommand(Long userId, UserRoleType role, MerchantData merchant) {}

    public record UpdateMerchantProfileCommand(Long userId, String businessName, String businessType) {}

    public record SubmitIdentityVerificationCommand(Long userId, String documentFrontS3Key, String documentBackS3Key) {}
}