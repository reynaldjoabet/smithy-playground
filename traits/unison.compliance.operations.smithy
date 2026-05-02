$version: "2.0"

namespace unison.compliance.operations

use unison.identity#authenticationRequired

// ============================================================================
// COMPLIANCE & REGULATORY OPERATIONS - Filings, Audits, Regulations
// ============================================================================
/// Submit regulatory filing.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["compliance:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/compliance/filings")
operation SubmitRegulatoryFiling {
    input: SubmitRegulatoryFilingInput
    output: SubmitRegulatoryFilingOutput
    errors: [
        Unauthorized
        InvalidFilingData
        DuplicateFiling
        ValidationFailed
    ]
}

structure SubmitRegulatoryFilingInput {
    @required
    filingType: String

    @required
    jurisdiction: String

    @required
    filingDate: Long

    @required
    data: String

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

structure SubmitRegulatoryFilingOutput {
    @required
    filingId: String

    @required
    filingReference: String

    @required
    submittedAt: Long
}

/// Get filing status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["compliance:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/compliance/filings/{filingId}")
operation GetFilingStatus {
    input: GetFilingStatusInput
    output: GetFilingStatusOutput
    errors: [
        Unauthorized
        FilingNotFound
    ]
}

structure GetFilingStatusInput {
    @required
    @httpLabel
    filingId: String
}

structure GetFilingStatusOutput {
    @required
    filing: Filing
}

structure Filing {
    @required
    filingId: String

    @required
    filingType: String

    @required
    jurisdiction: String

    @required
    status: String

    @required
    submittedAt: Long

    processedAt: Long

    rejectionReason: String

    filingReference: String
}

/// Schedule compliance audit.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["compliance:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/compliance/audits")
operation ScheduleComplianceAudit {
    input: ScheduleComplianceAuditInput
    output: ScheduleComplianceAuditOutput
    errors: [
        Unauthorized
        InvalidScope
    ]
}

structure ScheduleComplianceAuditInput {
    @required
    auditType: String

    @required
    scheduledDate: Long

    @required
    scope: String

    description: String
}

structure ScheduleComplianceAuditOutput {
    @required
    auditId: String

    @required
    scheduledDate: Long

    @required
    createdAt: Long
}

/// Get audit findings.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["compliance:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/compliance/audits/{auditId}/findings")
operation GetAuditFindings {
    input: GetAuditFindingsInput
    output: GetAuditFindingsOutput
    errors: [
        Unauthorized
        AuditNotFound
    ]
}

structure GetAuditFindingsInput {
    @required
    @httpLabel
    auditId: String
}

structure GetAuditFindingsOutput {
    @required
    audit: Audit

    @required
    findings: FindingList
}

structure Audit {
    @required
    auditId: String

    @required
    auditType: String

    @required
    status: String

    @required
    scheduledDate: Long

    completedDate: Long
}

list FindingList {
    member: Finding
}

structure Finding {
    @required
    findingId: String

    @required
    category: String

    @required
    severity: String

    @required
    description: String

    @required
    findings: String

    dueDate: Long
}

/// Report compliance issue or violation.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["compliance:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/compliance/issues")
operation ReportComplianceIssue {
    input: ReportComplianceIssueInput
    output: ReportComplianceIssueOutput
    errors: [
        Unauthorized
        InvalidIssue
    ]
}

structure ReportComplianceIssueInput {
    @required
    issueType: String

    @required
    severity: String

    @required
    description: String

    affectedEmployees: StringList

    evidenceDocuments: DocumentList
}

structure ReportComplianceIssueOutput {
    @required
    issueId: String

    @required
    reportedAt: Long
}

/// Get employee compliance status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["compliance:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/compliance/employees/{employeeId}/status")
operation GetEmployeeComplianceStatus {
    input: GetEmployeeComplianceStatusInput
    output: GetEmployeeComplianceStatusOutput
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
structure GetEmployeeComplianceStatusInput {
    @required
    @httpLabel
    employeeId: String
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure GetEmployeeComplianceStatusOutput {
    @required
    employeeId: String

    @required
    compliant: Boolean

    @required
    issues: ComplianceIssueList
}

list ComplianceIssueList {
    member: ComplianceIssue
}

structure ComplianceIssue {
    @required
    issueId: String

    @required
    type: String

    @required
    severity: String

    @required
    description: String

    @required
    reportedDate: Long

    resolutionDate: Long
}

/// List regulatory requirements.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["compliance:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/compliance/requirements")
operation ListRegulatoryRequirements {
    input: ListRegulatoryRequirementsInput
    output: ListRegulatoryRequirementsOutput
    errors: [
        Unauthorized
    ]
}

structure ListRegulatoryRequirementsInput {
    @httpQuery("jurisdiction")
    jurisdiction: String

    @httpQuery("category")
    category: String
}

structure ListRegulatoryRequirementsOutput {
    @required
    requirements: RequirementList
}

list RequirementList {
    member: RegulatoryRequirement
}

structure RegulatoryRequirement {
    @required
    requirementId: String

    @required
    jurisdiction: String

    @required
    name: String

    @required
    category: String

    @required
    description: String

    @required
    deadlineDate: Long

    status: String
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidFilingData {
    @required
    message: String

    invalidFields: StringList
}

@error("client")
structure DuplicateFiling {
    @required
    message: String

    existingFilingId: String
}

@error("client")
structure ValidationFailed {
    @required
    message: String

    errors: StringList
}

@error("client")
structure FilingNotFound {
    @required
    message: String
}

@error("client")
structure InvalidScope {
    @required
    message: String
}

@error("client")
structure AuditNotFound {
    @required
    message: String
}

@error("client")
structure InvalidIssue {
    @required
    message: String
}

@error("client")
structure EmployeeNotFound {
    @required
    message: String
}

list StringList {
    member: String
}
