$version: "2.0"

namespace unison.identity

/// Models the authentication requirements for an operation,
/// derived from the actual Unison Identity architecture:
/// OAuth2 PKCE flows, MFA enforcement, external provider SSO,
/// and feature-gated access.
@trait(selector: "operation")
structure authenticationRequired {
    /// Supported OAuth2 grant types for this operation
    allowedGrantTypes: GrantTypeList

    /// MFA enforcement level
    mfaPolicy: MFAPolicy

    /// Whether this operation requires an active SSO session
    /// (external providers: Azure AD, Okta, etc.)
    ssoRequired: Boolean

    /// Feature flag gate — operation is only accessible when flag is enabled
    /// Maps to UnisonIdentity.FeatureManagement
    featureFlag: String

    /// Token scope requirements beyond basic auth
    requiredScopes: StringList

    /// Session affinity — whether the operation must be pinned to
    /// the same Identity pod (for stateful MFA flows)
    sessionAffinity: SessionAffinity

    /// Maximum token age before re-authentication is forced (seconds)
    maxTokenAgeSec: Integer
}

list GrantTypeList {
    member: GrantType
}

enum GrantType {
    AUTHORIZATION_CODE_PKCE
    CLIENT_CREDENTIALS
    RESOURCE_OWNER
    DEVICE_CODE
    REFRESH_TOKEN
}

structure MFAPolicy {
    /// Whether MFA is required for this operation
    required: Boolean

    /// Allowed MFA methods
    allowedMethods: MFAMethodList

    /// Whether MFA can be remembered for this operation
    rememberDeviceAllowed: Boolean

    /// Remember duration in hours
    rememberDurationHours: Integer

    /// Step-up auth: require MFA even if session already has it,
    /// for high-sensitivity operations
    forceStepUp: Boolean
}

list MFAMethodList {
    member: MFAMethod
}

enum MFAMethod {
    TOTP
    SMS
    EMAIL
    PUSH_NOTIFICATION
    FIDO2_WEBAUTHN
    RECOVERY_CODE
}

enum SessionAffinity {
    NONE
    STICKY_SESSION
    DISTRIBUTED_SESSION
}

list StringList {
    member: String
}
