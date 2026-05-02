$version: "2.0"

namespace unison.employee.lifecycle

use unison.identity#authenticationRequired

// ============================================================================
// EMPLOYEE LIFECYCLE OPERATIONS - Hiring, Onboarding, Offboarding
// ============================================================================
/// Create a new job requisition for hiring.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["hiring:write", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/lifecycle/requisitions")
operation CreateJobRequisition {
    input: CreateJobRequisitionInput
    output: CreateJobRequisitionOutput
    errors: [
        Unauthorized
        InvalidInput
        DuplicateRequisition
    ]
}

structure CreateJobRequisitionInput {
    @required
    jobTitle: String

    @required
    department: String

    @required
    location: String

    @required
    salary: Salary

    jobDescription: String

    requiredSkills: StringList

    numberOfPositions: Integer = 1

    urgencyLevel: String
}

structure Salary {
    @required
    currency: String

    @required
    minAmount: Long

    @required
    maxAmount: Long

    frequency: String
}

structure CreateJobRequisitionOutput {
    @required
    requisitionId: String

    @required
    createdAt: Long

    status: String
}

/// List all job requisitions with filtering.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["hiring:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/lifecycle/requisitions")
operation ListJobRequisitions {
    input: ListJobRequisitionsInput
    output: ListJobRequisitionsOutput
    errors: [
        Unauthorized
    ]
}

structure ListJobRequisitionsInput {
    @httpQuery("status")
    status: String

    @httpQuery("department")
    department: String

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure ListJobRequisitionsOutput {
    @required
    requisitions: RequisitionList

    @required
    totalCount: Integer
}

list RequisitionList {
    member: JobRequisition
}

structure JobRequisition {
    @required
    requisitionId: String

    @required
    jobTitle: String

    @required
    department: String

    @required
    status: String

    @required
    createdAt: Long

    applicantCount: Integer

    filledPositions: Integer
}

/// Submit a job candidate application.
@http(method: "POST", uri: "/lifecycle/applications")
operation SubmitJobApplication {
    input: SubmitJobApplicationInput
    output: SubmitJobApplicationOutput
    errors: [
        InvalidInput
        RequisitionNotFound
        TooManyApplications
    ]
}

structure SubmitJobApplicationInput {
    @required
    requisitionId: String

    @required
    candidateName: String

    @required
    email: String

    @required
    resume: String

    phoneNumber: String

    coverLetter: String
}

structure SubmitJobApplicationOutput {
    @required
    applicationId: String

    @required
    submittedAt: Long

    status: String
}

/// Extend a job offer to a candidate.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["hiring:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/lifecycle/offers")
operation ExtendJobOffer {
    input: ExtendJobOfferInput
    output: ExtendJobOfferOutput
    errors: [
        Unauthorized
        ApplicationNotFound
        InvalidOfferTerms
    ]
}

structure ExtendJobOfferInput {
    @required
    applicationId: String

    @required
    salary: Long

    @required
    startDate: Long

    @required
    offerValidUntil: Long

    jobTitle: String

    benefits: StringList
}

structure ExtendJobOfferOutput {
    @required
    offerId: String

    @required
    offerLetter: String

    @required
    createdAt: Long
}

/// Accept or decline a job offer.
@http(method: "POST", uri: "/lifecycle/offers/{offerId}/respond")
operation RespondToJobOffer {
    input: RespondToJobOfferInput
    output: RespondToJobOfferOutput
    errors: [
        OfferNotFound
        OfferExpired
        InvalidResponse
    ]
}

structure RespondToJobOfferInput {
    @required
    @httpLabel
    offerId: String

    @required
    response: String

    notes: String
}

structure RespondToJobOfferOutput {
    @required
    accepted: Boolean

    @required
    respondedAt: Long
}

/// Create onboarding checklist for a new employee.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["onboarding:write", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/lifecycle/onboarding/checklists")
operation CreateOnboardingChecklist {
    input: CreateOnboardingChecklistInput
    output: CreateOnboardingChecklistOutput
    errors: [
        Unauthorized
        EmployeeNotFound
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure CreateOnboardingChecklistInput {
    @required
    employeeId: String

    @required
    startDate: Long

    checklistTemplate: String

    assignedTo: StringList
}

structure CreateOnboardingChecklistOutput {
    @required
    checklistId: String

    @required
    createdAt: Long

    @required
    items: OnboardingItemList
}

list OnboardingItemList {
    member: OnboardingItem
}

structure OnboardingItem {
    @required
    itemId: String

    @required
    title: String

    @required
    dueDate: Long

    status: String

    assignedTo: String
}

/// Complete an onboarding checklist item.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["onboarding:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/lifecycle/onboarding/items/{itemId}/complete")
operation CompleteOnboardingItem {
    input: CompleteOnboardingItemInput
    output: CompleteOnboardingItemOutput
    errors: [
        ItemNotFound
        InvalidState
    ]
}

structure CompleteOnboardingItemInput {
    @required
    @httpLabel
    itemId: String

    notes: String

    attachments: StringList
}

structure CompleteOnboardingItemOutput {
    @required
    completed: Boolean

    @required
    completedAt: Long
}

/// Initiate employee offboarding process.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["offboarding:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/lifecycle/offboarding")
operation InitiateOffboarding {
    input: InitiateOffboardingInput
    output: InitiateOffboardingOutput
    errors: [
        Unauthorized
        EmployeeNotFound
        InvalidTransition
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure InitiateOffboardingInput {
    @required
    employeeId: String

    @required
    lastWorkDay: Long

    reason: String

    returnEquipment: StringList

    exitInterviewRequired: Boolean
}

structure InitiateOffboardingOutput {
    @required
    offboardingId: String

    @required
    createdAt: Long

    @required
    checklist: OnboardingItemList
}

/// Get employee lifecycle timeline (hiring to offboarding).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["employee:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/lifecycle/employees/{employeeId}/timeline")
operation GetEmployeeLifecycleTimeline {
    input: GetEmployeeLifecycleTimelineInput
    output: GetEmployeeLifecycleTimelineOutput
    errors: [
        Unauthorized
        EmployeeNotFound
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure GetEmployeeLifecycleTimelineInput {
    @required
    @httpLabel
    employeeId: String
}

structure GetEmployeeLifecycleTimelineOutput {
    @required
    events: LifecycleEventList
}

list LifecycleEventList {
    member: LifecycleEvent
}

structure LifecycleEvent {
    @required
    eventId: String

    @required
    eventType: String

    @required
    timestamp: Long

    @required
    description: String

    metadata: String
}

// ============================================================================
// ERROR SHAPES
// ============================================================================
@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidInput {
    @required
    message: String

    invalidFields: StringList
}

@error("client")
structure DuplicateRequisition {
    @required
    message: String
}

@error("client")
structure RequisitionNotFound {
    @required
    message: String
}

@error("server")
structure TooManyApplications {
    @required
    message: String

    retryAfterSeconds: Integer
}

@error("client")
structure ApplicationNotFound {
    @required
    message: String
}

@error("client")
structure InvalidOfferTerms {
    @required
    message: String
}

@error("client")
structure OfferNotFound {
    @required
    message: String
}

@error("client")
structure OfferExpired {
    @required
    message: String
}

@error("client")
structure InvalidResponse {
    @required
    message: String
}

@error("client")
structure EmployeeNotFound {
    @required
    message: String
}

@error("client")
structure ItemNotFound {
    @required
    message: String
}

@error("client")
structure InvalidState {
    @required
    message: String
}

@error("client")
structure InvalidTransition {
    @required
    message: String
}

list StringList {
    member: String
}
