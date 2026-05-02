$version: "2.0"

namespace unison.identity.operations

use unison.identity#MFAMethod
use unison.identity#authenticationRequired

/// Get the currently authenticated user's profile and metadata.
/// Requires OAuth2 PKCE or refresh token, with optional MFA.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: {
        required: false
        allowedMethods: ["TOTP", "SMS", "EMAIL"]
        rememberDeviceAllowed: true
        rememberDurationHours: 24
        forceStepUp: false
    }
    ssoRequired: true
    requiredScopes: ["profile", "email"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/me")
operation GetCurrentUser {
    input: GetCurrentUserInput
    output: GetCurrentUserOutput
    errors: [
        Unauthorized
    ]
}

structure GetCurrentUserInput {}

structure GetCurrentUserOutput {
    @required
    userId: String

    @required
    email: String

    displayName: String

    tenantId: String

    @required
    issuedAt: Long

    @required
    expiresAt: Long
}

/// Initiate a multi-factor authentication challenge.
/// Requires client credentials with forced step-up MFA.
/// Sends a code via the specified method (SMS, TOTP, email, etc.).
@authenticationRequired(
    allowedGrantTypes: ["CLIENT_CREDENTIALS"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "FIDO2_WEBAUTHN"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["admin:mfa", "security:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/identity/mfa/initiate")
operation InitiateMFA {
    input: InitiateMFAInput
    output: InitiateMFAOutput
    errors: [
        Unauthorized
        TooManyRequests
    ]
}

structure InitiateMFAInput {
    @required
    userId: String

    @required
    mfaMethod: MFAMethod
}

structure InitiateMFAOutput {
    @required
    challengeId: String

    @required
    expiresAt: Long

    deliveryMethod: String
}

/// Verify an MFA challenge by submitting the code.
/// Returns a session token if verification succeeds.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "SMS", "FIDO2_WEBAUTHN", "RECOVERY_CODE"]
    }
    ssoRequired: false
    requiredScopes: ["auth:verify"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 300
)
@http(method: "POST", uri: "/identity/mfa/verify")
operation VerifyMFA {
    input: VerifyMFAInput
    output: VerifyMFAOutput
    errors: [
        Unauthorized
        InvalidMFACode
    ]
}

structure VerifyMFAInput {
    @required
    challengeId: String

    @required
    code: String
}

structure VerifyMFAOutput {
    @required
    sessionToken: String

    @required
    expiresAt: Long
}

/// Refresh an access token using a refresh token.
/// Used to obtain a new access token without requiring user re-authentication.
@authenticationRequired(
    allowedGrantTypes: ["REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["auth:refresh"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 7200
)
@http(method: "POST", uri: "/identity/token/refresh")
operation RefreshToken {
    input: RefreshTokenInput
    output: RefreshTokenOutput
    errors: [
        Unauthorized
        TokenExpired
    ]
}

structure RefreshTokenInput {
    @required
    refreshToken: String
}

structure RefreshTokenOutput {
    @required
    accessToken: String

    @required
    expiresIn: Integer
}

/// Revoke an active access or refresh token.
/// Typically used on logout to invalidate the session.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "CLIENT_CREDENTIALS", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["auth:revoke"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/identity/token/revoke")
operation RevokeToken {
    input: RevokeTokenInput
    output: RevokeTokenOutput
    errors: [
        Unauthorized
    ]
}

structure RevokeTokenInput {
    @required
    token: String
}

structure RevokeTokenOutput {
    @required
    revoked: Boolean
}

// Error shapes used by identity operations
@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidMFACode {
    @required
    message: String

    attemptsRemaining: Integer
}

@error("server")
structure TooManyRequests {
    @required
    message: String

    retryAfterSeconds: Integer
}

@error("client")
structure TokenExpired {
    @required
    message: String
}

// ============================================================================
// PASSWORD MANAGEMENT OPERATIONS
// ============================================================================
/// Change the current user's password.
/// Requires the user to provide their current password for verification.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: false
        allowedMethods: ["TOTP", "SMS"]
    }
    ssoRequired: false
    requiredScopes: ["password:write", "profile:read"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/identity/password/change")
operation ChangePassword {
    input: ChangePasswordInput
    output: ChangePasswordOutput
    errors: [
        Unauthorized
        InvalidPassword
        PasswordRequirementsNotMet
    ]
}

structure ChangePasswordInput {
    @required
    currentPassword: String

    @required
    newPassword: String
}

structure ChangePasswordOutput {
    @required
    changed: Boolean

    @required
    expiresAt: Long
}

/// Request a password reset by email.
/// Sends a reset link to the user's registered email address.
@http(method: "POST", uri: "/identity/password/reset-request")
operation RequestPasswordReset {
    input: RequestPasswordResetInput
    output: RequestPasswordResetOutput
    errors: [
        UserNotFound
        TooManyRequests
    ]
}

structure RequestPasswordResetInput {
    @required
    email: String
}

structure RequestPasswordResetOutput {
    @required
    resetSent: Boolean

    @required
    expiresAt: Long
}

/// Complete a password reset using a reset token.
/// The token must be valid and not expired.
@http(method: "POST", uri: "/identity/password/reset-confirm")
operation ConfirmPasswordReset {
    input: ConfirmPasswordResetInput
    output: ConfirmPasswordResetOutput
    errors: [
        InvalidResetToken
        TokenExpired
        PasswordRequirementsNotMet
    ]
}

structure ConfirmPasswordResetInput {
    @required
    resetToken: String

    @required
    newPassword: String
}

structure ConfirmPasswordResetOutput {
    @required
    confirmed: Boolean

    @required
    sessionToken: String
}

// ============================================================================
// MFA ENROLLMENT OPERATIONS
// ============================================================================
/// List all MFA methods enrolled by the current user.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["mfa:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/mfa/methods")
operation ListMFAMethods {
    input: ListMFAMethodsInput
    output: ListMFAMethodsOutput
    errors: [
        Unauthorized
    ]
}

structure ListMFAMethodsInput {}

structure ListMFAMethodsOutput {
    @required
    methods: MFAMethodList

    enrolledAt: Long

    lastUsedAt: Long
}

/// Enroll a new MFA method (e.g., TOTP app, security key).
/// Returns a setup challenge that the user must complete.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: false
        allowedMethods: ["TOTP", "SMS", "EMAIL"]
    }
    ssoRequired: false
    requiredScopes: ["mfa:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/identity/mfa/enroll")
operation EnrollMFA {
    input: EnrollMFAInput
    output: EnrollMFAOutput
    errors: [
        Unauthorized
        MFAMethodAlreadyEnrolled
    ]
}

structure EnrollMFAInput {
    @required
    mfaMethod: MFAMethod
}

structure EnrollMFAOutput {
    @required
    challengeId: String

    @required
    setupData: String

    @required
    expiresAt: Long
}

/// Disable an enrolled MFA method.
/// Requires authentication and possibly MFA verification.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "SMS", "FIDO2_WEBAUTHN"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["mfa:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/identity/mfa/disable")
operation DisableMFA {
    input: DisableMFAInput
    output: DisableMFAOutput
    errors: [
        Unauthorized
        MFAMethodNotFound
        CannotDisableLastMethod
    ]
}

structure DisableMFAInput {
    @required
    mfaMethod: MFAMethod
}

structure DisableMFAOutput {
    @required
    disabled: Boolean
}

// ============================================================================
// TOKEN VALIDATION OPERATIONS
// ============================================================================
/// Validate/introspect an access token (service-to-service).
/// Returns token claims and validity information without authentication.
@http(method: "POST", uri: "/identity/token/validate")
operation ValidateToken {
    input: ValidateTokenInput
    output: ValidateTokenOutput
}

structure ValidateTokenInput {
    @required
    token: String
}

structure ValidateTokenOutput {
    @required
    valid: Boolean

    @required
    userId: String

    @required
    expiresAt: Long

    scopes: StringList

    tenantId: String
}

// ============================================================================
// SESSION MANAGEMENT OPERATIONS
// ============================================================================
/// List all active sessions for the current user.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["sessions:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/sessions")
operation ListActiveSessions {
    input: ListActiveSessionsInput
    output: ListActiveSessionsOutput
    errors: [
        Unauthorized
    ]
}

structure ListActiveSessionsInput {}

structure ListActiveSessionsOutput {
    @required
    sessions: SessionList

    @required
    totalCount: Integer
}

list SessionList {
    member: UserSession
}

structure UserSession {
    @required
    sessionId: String

    @required
    userId: String

    @required
    createdAt: Long

    @required
    lastActivityAt: Long

    @required
    expiresAt: Long

    ipAddress: String

    userAgent: String

    isCurrentSession: Boolean
}

/// Revoke a specific user session by ID (e.g., logout from another device).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["sessions:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/identity/sessions/{sessionId}/revoke")
operation RevokeSession {
    input: RevokeSessionInput
    output: RevokeSessionOutput
    errors: [
        Unauthorized
        SessionNotFound
    ]
}

structure RevokeSessionInput {
    @required
    @httpLabel
    sessionId: String
}

structure RevokeSessionOutput {
    @required
    revoked: Boolean
}

// ============================================================================
// PROFILE OPERATIONS
// ============================================================================
/// Update the current user's profile information.
/// Allows changing displayName and other non-sensitive profile fields.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["profile:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "PATCH", uri: "/identity/profile")
operation UpdateProfile {
    input: UpdateProfileInput
    output: UpdateProfileOutput
    errors: [
        Unauthorized
        InvalidProfileData
    ]
}

structure UpdateProfileInput {
    displayName: String
    phoneNumber: String
    locale: String
    timezone: String
}

structure UpdateProfileOutput {
    @required
    userId: String

    @required
    displayName: String

    @required
    updatedAt: Long
}

// ============================================================================
// AUDIT & COMPLIANCE OPERATIONS
// ============================================================================
/// Get audit log for the current user's account.
/// Lists all actions taken on the account (logins, password changes, etc).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["audit:read", "security:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/audit/log")
operation GetAuditLog {
    input: GetAuditLogInput
    output: GetAuditLogOutput
    errors: [
        Unauthorized
    ]
}

structure GetAuditLogInput {
    @httpQuery("limit")
    limit: Integer = 100

    @httpQuery("offset")
    offset: Integer = 0

    @httpQuery("startTime")
    startTime: Long

    @httpQuery("endTime")
    endTime: Long

    @httpQuery("eventTypes")
    eventTypes: StringList
}

structure GetAuditLogOutput {
    @required
    events: AuditEventList

    @required
    totalCount: Integer

    hasMore: Boolean
}

list AuditEventList {
    member: AuditEvent
}

structure AuditEvent {
    @required
    eventId: String

    @required
    eventType: String

    @required
    timestamp: Long

    @required
    userId: String

    ipAddress: String

    userAgent: String

    resourceType: String

    resourceId: String

    status: String

    description: String
}

/// Get recent login history for the current user.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["security:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/logins/history")
operation GetLoginHistory {
    input: GetLoginHistoryInput
    output: GetLoginHistoryOutput
    errors: [
        Unauthorized
    ]
}

structure GetLoginHistoryInput {
    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetLoginHistoryOutput {
    @required
    logins: LoginEventList

    @required
    totalCount: Integer
}

list LoginEventList {
    member: LoginEvent
}

structure LoginEvent {
    @required
    loginId: String

    @required
    timestamp: Long

    @required
    ipAddress: String

    @required
    status: String

    userAgent: String

    geoLocation: String

    mfaUsed: Boolean
}

/// Get aggregate view of all recent account activity.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["audit:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/account/activity")
operation GetAccountActivity {
    input: GetAccountActivityInput
    output: GetAccountActivityOutput
    errors: [
        Unauthorized
    ]
}

structure GetAccountActivityInput {
    @httpQuery("days")
    days: Integer = 30
}

structure GetAccountActivityOutput {
    @required
    lastLogin: Long

    lastPasswordChange: Long

    lastMFAChange: Long

    lastProfileUpdate: Long

    loginCount: Integer

    failedLoginAttempts: Integer

    activeSessionCount: Integer
}

// ============================================================================
// DEVICE TRUST & PASSWORDLESS OPERATIONS
// ============================================================================
/// Register a new trusted device for passwordless auth or remember-me functionality.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "SMS"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["devices:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/identity/devices/register")
operation RegisterDevice {
    input: RegisterDeviceInput
    output: RegisterDeviceOutput
    errors: [
        Unauthorized
        TooManyRequests
    ]
}

structure RegisterDeviceInput {
    @required
    deviceName: String

    deviceType: String

    publicKey: String
}

structure RegisterDeviceOutput {
    @required
    deviceId: String

    @required
    registeredAt: Long

    trustToken: String
}

/// List all trusted devices registered by the current user.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["devices:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/devices/trusted")
operation ListTrustedDevices {
    input: ListTrustedDevicesInput
    output: ListTrustedDevicesOutput
    errors: [
        Unauthorized
    ]
}

structure ListTrustedDevicesInput {}

structure ListTrustedDevicesOutput {
    @required
    devices: TrustedDeviceList
}

list TrustedDeviceList {
    member: TrustedDevice
}

structure TrustedDevice {
    @required
    deviceId: String

    @required
    deviceName: String

    @required
    registeredAt: Long

    @required
    lastSeenAt: Long

    deviceType: String

    isCurrentDevice: Boolean
}

/// Remove a device from the trusted devices list.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["devices:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/identity/devices/{deviceId}/untrust")
operation UntrustDevice {
    input: UntrustDeviceInput
    output: UntrustDeviceOutput
    errors: [
        Unauthorized
        DeviceNotFound
    ]
}

structure UntrustDeviceInput {
    @required
    @httpLabel
    deviceId: String
}

structure UntrustDeviceOutput {
    @required
    untrusted: Boolean
}

/// Initiate passwordless login with magic link or one-time code.
@http(method: "POST", uri: "/identity/passwordless/initiate")
operation InitiatePasswordlessLogin {
    input: InitiatePasswordlessLoginInput
    output: InitiatePasswordlessLoginOutput
    errors: [
        UserNotFound
        TooManyRequests
    ]
}

structure InitiatePasswordlessLoginInput {
    @required
    email: String

    deliveryMethod: String
}

structure InitiatePasswordlessLoginOutput {
    @required
    challengeId: String

    @required
    expiresAt: Long

    deliveryMethod: String
}

/// Verify passwordless login challenge (magic link / code).
@http(method: "POST", uri: "/identity/passwordless/verify")
operation VerifyPasswordlessLogin {
    input: VerifyPasswordlessLoginInput
    output: VerifyPasswordlessLoginOutput
    errors: [
        InvalidChallenge
        TokenExpired
        TooManyRequests
    ]
}

structure VerifyPasswordlessLoginInput {
    @required
    challengeId: String

    @required
    code: String
}

structure VerifyPasswordlessLoginOutput {
    @required
    sessionToken: String

    @required
    expiresAt: Long

    mfaRequired: Boolean
}

// ============================================================================
// ACCOUNT RECOVERY OPERATIONS
// ============================================================================
/// List backup recovery codes for account recovery (one-time use).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "FIDO2_WEBAUTHN"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["recovery:read"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@readonly
@http(method: "GET", uri: "/identity/recovery/codes")
operation ListRecoveryCodes {
    input: ListRecoveryCodesInput
    output: ListRecoveryCodesOutput
    errors: [
        Unauthorized
    ]
}

structure ListRecoveryCodesInput {}

structure ListRecoveryCodesOutput {
    @required
    codes: RecoveryCodeList

    @required
    generatedAt: Long

    @required
    unusedCount: Integer
}

list RecoveryCodeList {
    member: RecoveryCode
}

structure RecoveryCode {
    @required
    code: String

    used: Boolean

    usedAt: Long
}

/// Generate a new set of recovery codes.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "SMS", "FIDO2_WEBAUTHN"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["recovery:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/identity/recovery/codes/generate")
operation GenerateRecoveryCodes {
    input: GenerateRecoveryCodesInput
    output: GenerateRecoveryCodesOutput
    errors: [
        Unauthorized
    ]
}

structure GenerateRecoveryCodesInput {}

structure GenerateRecoveryCodesOutput {
    @required
    codes: RecoveryCodeList

    @required
    generatedAt: Long
}

// ============================================================================
// DELEGATION OPERATIONS
// ============================================================================
/// Get permissions delegated to the current user by others.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["delegation:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/delegation/received")
operation GetDelegatedPermissions {
    input: GetDelegatedPermissionsInput
    output: GetDelegatedPermissionsOutput
    errors: [
        Unauthorized
    ]
}

structure GetDelegatedPermissionsInput {}

structure GetDelegatedPermissionsOutput {
    @required
    delegations: DelegationList

    @required
    totalCount: Integer
}

list DelegationList {
    member: DelegationGrant
}

structure DelegationGrant {
    @required
    delegationId: String

    @required
    delegatedBy: String

    @required
    delegatedAt: Long

    @required
    expiresAt: Long

    scopes: StringList

    permissionLevel: String
}

/// Grant delegation to another user (allow them to act on your behalf).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["delegation:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/identity/delegation/grant")
operation GrantDelegation {
    input: GrantDelegationInput
    output: GrantDelegationOutput
    errors: [
        Unauthorized
        UserNotFound
        InvalidDelegationScope
    ]
}

structure GrantDelegationInput {
    @required
    targetUserId: String

    @required
    scopes: StringList

    @required
    expiresAt: Long

    reason: String
}

structure GrantDelegationOutput {
    @required
    delegationId: String

    @required
    grantedAt: Long
}

/// Revoke a previously granted delegation.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["delegation:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/identity/delegation/{delegationId}/revoke")
operation RevokeDelegation {
    input: RevokeDelegationInput
    output: RevokeDelegationOutput
    errors: [
        Unauthorized
        DelegationNotFound
    ]
}

structure RevokeDelegationInput {
    @required
    @httpLabel
    delegationId: String
}

structure RevokeDelegationOutput {
    @required
    revoked: Boolean
}

// ============================================================================
// SECURITY POLICY OPERATIONS
// ============================================================================
/// Get security policies applicable to the current user.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["security:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/security/policies")
operation GetSecurityPolicy {
    input: GetSecurityPolicyInput
    output: GetSecurityPolicyOutput
    errors: [
        Unauthorized
    ]
}

structure GetSecurityPolicyInput {}

structure GetSecurityPolicyOutput {
    @required
    passwordExpiryDays: Integer

    @required
    mfaMandatory: Boolean

    @required
    minPasswordLength: Integer

    sessionTimeoutMinutes: Integer

    allowWeakPasswords: Boolean

    ipWhitelist: StringList

    requireSecurityQuestions: Boolean
}

/// Update organization security policies (admin operation).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP", "FIDO2_WEBAUTHN"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["admin:write", "security:write"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "PATCH", uri: "/identity/security/policies")
operation UpdateSecurityPolicy {
    input: UpdateSecurityPolicyInput
    output: UpdateSecurityPolicyOutput
    errors: [
        Unauthorized
        InvalidPolicyValue
    ]
}

structure UpdateSecurityPolicyInput {
    passwordExpiryDays: Integer
    mfaMandatory: Boolean
    minPasswordLength: Integer
    sessionTimeoutMinutes: Integer
    ipWhitelist: StringList
}

structure UpdateSecurityPolicyOutput {
    @required
    updated: Boolean

    @required
    appliedAt: Long
}

// ============================================================================
// MULTI-TENANT / ORGANIZATION OPERATIONS
// ============================================================================
/// List all organizations the current user belongs to.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["org:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/identity/organizations")
operation ListOrganizations {
    input: ListOrganizationsInput
    output: ListOrganizationsOutput
    errors: [
        Unauthorized
    ]
}

structure ListOrganizationsInput {}

structure ListOrganizationsOutput {
    @required
    organizations: OrganizationList

    @required
    totalCount: Integer
}

list OrganizationList {
    member: Organization
}

structure Organization {
    @required
    organizationId: String

    @required
    name: String

    @required
    role: String

    @required
    joinedAt: Long

    isActive: Boolean
}

/// Switch the active organization context for the current session.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["org:switch"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@http(method: "POST", uri: "/identity/organizations/{organizationId}/switch")
operation SwitchOrganization {
    input: SwitchOrganizationInput
    output: SwitchOrganizationOutput
    errors: [
        Unauthorized
        OrganizationNotFound
        AccessDenied
    ]
}

structure SwitchOrganizationInput {
    @required
    @httpLabel
    organizationId: String
}

structure SwitchOrganizationOutput {
    @required
    switched: Boolean

    @required
    activeOrganization: String

    sessionToken: String
}

// ============================================================================
// ADDITIONAL ERROR SHAPES
// ============================================================================
@error("client")
structure InvalidPassword {
    @required
    message: String
}

@error("client")
structure PasswordRequirementsNotMet {
    @required
    message: String

    requirements: StringList
}

@error("client")
structure UserNotFound {
    @required
    message: String
}

@error("client")
structure InvalidResetToken {
    @required
    message: String
}

@error("client")
structure MFAMethodAlreadyEnrolled {
    @required
    message: String
}

@error("client")
structure MFAMethodNotFound {
    @required
    message: String
}

@error("client")
structure CannotDisableLastMethod {
    @required
    message: String
}

@error("client")
structure SessionNotFound {
    @required
    message: String
}

@error("client")
structure InvalidProfileData {
    @required
    message: String

    invalidFields: StringList
}

@error("client")
structure InvalidPassword {
    @required
    message: String
}

@error("client")
structure PasswordRequirementsNotMet {
    @required
    message: String

    requirements: StringList
}

@error("client")
structure UserNotFound {
    @required
    message: String
}

@error("client")
structure InvalidResetToken {
    @required
    message: String
}

@error("client")
structure MFAMethodAlreadyEnrolled {
    @required
    message: String
}

@error("client")
structure MFAMethodNotFound {
    @required
    message: String
}

@error("client")
structure CannotDisableLastMethod {
    @required
    message: String
}

@error("client")
structure SessionNotFound {
    @required
    message: String
}

@error("client")
structure DeviceNotFound {
    @required
    message: String
}

@error("client")
structure InvalidChallenge {
    @required
    message: String
}

@error("client")
structure InvalidDelegationScope {
    @required
    message: String
}

@error("client")
structure DelegationNotFound {
    @required
    message: String
}

@error("client")
structure InvalidPolicyValue {
    @required
    message: String

    invalidFields: StringList
}

@error("client")
structure OrganizationNotFound {
    @required
    message: String
}

@error("client")
structure AccessDenied {
    @required
    message: String
}

list StringList {
    member: String
}

list MFAMethodList {
    member: MFAMethod
}
