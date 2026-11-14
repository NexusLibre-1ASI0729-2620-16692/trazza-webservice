package com.trazza.iam.domain.model.valueobjects;

public class IamEnums {
    // No instanciable
    private IamEnums() {}

    /**
     * Role of an account. One account can be carrier and merchant at the same time (user_roles).
     */
    public enum UserRoleType {
        CARRIER,
        MERCHANT
    }

    /**
     * Identity validation state of a person (person_identities.kyc_status).
     */
    public enum KycStatus {
        PENDING,
        VALIDATED,
        REJECTED
    }

    /**
     * State of one identity verification attempt (identity_verifications.status).
     */
    public enum VerificationStatus {
        SUBMITTED,
        VALIDATED,
        REJECTED
    }

    /**
     * Optional sex of a fleet driver (drivers.sex).
     */
    public enum DriverSex {
        F,
        M,
        X
    }

    /**
     * Role names used by the Authorize attribute.
     */
    public static final class Roles {
        private Roles() {}
        public static final String CARRIER = "CARRIER";
        public static final String MERCHANT = "MERCHANT";
    }
}