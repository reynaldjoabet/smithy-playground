$version: "2.0"
namespace unison.identity
use unison#tenantScoped
use unison.operations#auditable
use unison.identity#authenticationRequired
use unison.encryption#encryptedField

@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "FIDO2_WEBAUTHN", "PUSH_NOTIFICATION"]
        rememberDeviceAllowed: false
        rememberDurationHours: 0
        forceStepUp: true
    }
    ssoRequired: false
    featureFlag: ""
    requiredScopes: ["identity:write", "profile:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 300
)
@tenantScoped
@auditable(sensitivity: "CRITICAL", retentionDays: 2555)
@http(method: "POST", uri: "/identity/password/change")
operation ChangePassword {
    input: ChangePasswordInput
    output: ChangePasswordOutput
    errors: [
        InvalidCurrentPassword
        PasswordPolicyViolation
        MFARequired
        MFAChallengeFailed
        SessionExpired
        AccountLocked
    ]
}

structure ChangePasswordInput {
    @required
    @encryptedField(
        encryptionContext: "identity-credentials"
        classification: "SPII"
        masking: "FULL_MASK"
    )
    currentPassword: String
    @required
    @encryptedField(
        encryptionContext: "identity-credentials"
        classification: "SPII"
        masking: "FULL_MASK"
    )
    newPassword: String
}

structure ChangePasswordOutput {
    @required
    success: Boolean
}

@error("client")
structure InvalidCurrentPassword {
    @required
    message: String
}

@error("client")
structure PasswordPolicyViolation {
    @required
    message: String
    reason: String
}

@error("client")
structure MFARequired {
    @required
    message: String
    challengeId: String
}

@error("client")
structure MFAChallengeFailed {
    @required
    message: String
}

@error("server")
structure SessionExpired {
    @required
    message: String
}

@error("server")
structure AccountLocked {
    @required
    message: String
    retryAfterSeconds: Integer
}
