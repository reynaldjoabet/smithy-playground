$version: "2.0"

namespace unison.benefits.operations

use unison.identity#authenticationRequired

// ============================================================================
// BENEFITS MANAGEMENT OPERATIONS - Enrollment, Claims, Coverage
// ============================================================================
/// Get available benefits plans.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/benefits/plans")
operation GetAvailableBenefits {
    input: GetAvailableBenefitsInput
    output: GetAvailableBenefitsOutput
    errors: [
        Unauthorized
    ]
}

structure GetAvailableBenefitsInput {
    @httpQuery("employmentType")
    employmentType: String
}

structure GetAvailableBenefitsOutput {
    @required
    plans: BenefitPlanList
}

list BenefitPlanList {
    member: BenefitPlan
}

structure BenefitPlan {
    @required
    planId: String

    @required
    name: String

    @required
    category: String

    @required
    description: String

    @required
    isActive: Boolean

    premium: String

    deductible: String

    coverage: String
}

/// Enroll in a benefit plan.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/benefits/enroll")
operation EnrollBenefit {
    input: EnrollBenefitInput
    output: EnrollBenefitOutput
    errors: [
        Unauthorized
        PlanNotFound
        NotEligible
        EnrollmentLocked
    ]
}

structure EnrollBenefitInput {
    @required
    planId: String

    @required
    coverageLevel: String

    dependents: DependentList

    beneficiaries: BeneficiaryList
}

list DependentList {
    member: Dependent
}

structure Dependent {
    @required
    name: String

    @required
    relationship: String

    @required
    dateOfBirth: Long
}

list BeneficiaryList {
    member: Beneficiary
}

structure Beneficiary {
    @required
    name: String

    @required
    relationship: String

    @required
    percentage: Double
}

structure EnrollBenefitOutput {
    @required
    enrollmentId: String

    @required
    planId: String

    @required
    enrolledAt: Long

    @required
    effectiveDate: Long
}

/// Get employee's current benefits enrollment.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/benefits/enrollments")
operation GetBenefitEnrollments {
    input: GetBenefitEnrollmentsInput
    output: GetBenefitEnrollmentsOutput
    errors: [
        Unauthorized
    ]
}

structure GetBenefitEnrollmentsInput {
    @httpQuery("status")
    status: String
}

structure GetBenefitEnrollmentsOutput {
    @required
    enrollments: BenefitEnrollmentList
}

list BenefitEnrollmentList {
    member: BenefitEnrollment
}

structure BenefitEnrollment {
    @required
    enrollmentId: String

    @required
    planId: String

    @required
    planName: String

    @required
    status: String

    @required
    enrolledAt: Long

    @required
    effectiveDate: Long

    coverageLevel: String
}

/// File a benefits claim.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/benefits/claims")
operation FileClaimRequest {
    input: FileClaimRequestInput
    output: FileClaimRequestOutput
    errors: [
        Unauthorized
        NoActiveEnrollment
        InvalidClaimData
    ]
}

structure FileClaimRequestInput {
    @required
    enrollmentId: String

    @required
    claimDate: Long

    @required
    claimAmount: Double

    @required
    description: String

    supportingDocuments: DocumentList
}

list DocumentList {
    member: Document
}

structure Document {
    @required
    name: String

    @required
    content: String

    @required
    mimeType: String
}

structure FileClaimRequestOutput {
    @required
    claimId: String

    @required
    filedAt: Long

    @required
    status: String

    estimatedProcessingDays: Integer
}

/// Get claim status and history.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/benefits/claims")
operation GetClaimHistory {
    input: GetClaimHistoryInput
    output: GetClaimHistoryOutput
    errors: [
        Unauthorized
    ]
}

structure GetClaimHistoryInput {
    @httpQuery("status")
    status: String

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetClaimHistoryOutput {
    @required
    claims: ClaimList

    @required
    totalCount: Integer
}

list ClaimList {
    member: Claim
}

structure Claim {
    @required
    claimId: String

    @required
    enrollmentId: String

    @required
    claimDate: Long

    @required
    claimAmount: Double

    @required
    status: String

    @required
    filedAt: Long

    processedAt: Long

    approvedAmount: Double
}

/// Update beneficiary information.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "PATCH", uri: "/benefits/enrollments/{enrollmentId}/beneficiaries")
operation UpdateBeneficiaries {
    input: UpdateBeneficiariesInput
    output: UpdateBeneficiariesOutput
    errors: [
        Unauthorized
        EnrollmentNotFound
        InvalidData
    ]
}

structure UpdateBeneficiariesInput {
    @required
    @httpLabel
    enrollmentId: String

    @required
    beneficiaries: BeneficiaryList
}

structure UpdateBeneficiariesOutput {
    @required
    updated: Boolean

    @required
    updatedAt: Long
}

/// Get deductions and contributions breakdown.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["benefits:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/benefits/deductions")
operation GetDeductionsBreakdown {
    input: GetDeductionsBreakdownInput
    output: GetDeductionsBreakdownOutput
    errors: [
        Unauthorized
    ]
}

structure GetDeductionsBreakdownInput {
    @httpQuery("payPeriod")
    payPeriod: Long
}

structure GetDeductionsBreakdownOutput {
    @required
    deductions: DeductionList

    @required
    totalDeductions: Double

    @required
    netPay: Double
}

list DeductionList {
    member: Deduction
}

structure Deduction {
    @required
    name: String

    @required
    category: String

    @required
    amount: Double

    @required
    frequency: String
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure PlanNotFound {
    @required
    message: String
}

@error("client")
structure NotEligible {
    @required
    message: String
}

@error("client")
structure EnrollmentLocked {
    @required
    message: String

    reopenDate: Long
}

@error("client")
structure NoActiveEnrollment {
    @required
    message: String
}

@error("client")
structure InvalidClaimData {
    @required
    message: String

    invalidFields: StringList
}

@error("client")
structure EnrollmentNotFound {
    @required
    message: String
}

@error("client")
structure InvalidData {
    @required
    message: String
}

list StringList {
    member: String
}
