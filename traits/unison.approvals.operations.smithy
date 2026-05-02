$version: "2.0"

namespace unison.approvals.operations

use unison.identity#authenticationRequired

// ============================================================================
// WORKFLOW APPROVALS OPERATIONS - Approval Chains, Workflows
// ============================================================================
/// Create approval workflow.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:write", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/approvals/workflows")
operation CreateApprovalWorkflow {
    input: CreateApprovalWorkflowInput
    output: CreateApprovalWorkflowOutput
    errors: [
        Unauthorized
        InvalidWorkflow
    ]
}

structure CreateApprovalWorkflowInput {
    @required
    workflowName: String

    @required
    workflowType: String

    @required
    approvers: ApproverList

    @required
    requireAllApprovals: Boolean

    allowRejection: Boolean = true

    notificationTemplate: String
}

list ApproverList {
    member: Approver
}

structure Approver {
    @required
    approverId: String

    @required
    order: Integer

    @required
    role: String

    requireComment: Boolean = false
}

structure CreateApprovalWorkflowOutput {
    @required
    workflowId: String

    @required
    createdAt: Long
}

/// Submit request for approval.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/approvals/requests")
operation SubmitApprovalRequest {
    input: SubmitApprovalRequestInput
    output: SubmitApprovalRequestOutput
    errors: [
        Unauthorized
        InvalidRequest
        WorkflowNotFound
    ]
}

structure SubmitApprovalRequestInput {
    @required
    workflowId: String

    @required
    requestType: String

    @required
    data: String

    reason: String
}

structure SubmitApprovalRequestOutput {
    @required
    requestId: String

    @required
    submittedAt: Long

    @required
    status: String
}

/// Get pending approvals for user.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/approvals/pending")
operation GetPendingApprovals {
    input: GetPendingApprovalsInput
    output: GetPendingApprovalsOutput
    errors: [
        Unauthorized
    ]
}

structure GetPendingApprovalsInput {
    @httpQuery("requestType")
    requestType: String

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetPendingApprovalsOutput {
    @required
    approvals: PendingApprovalList

    @required
    totalCount: Integer
}

list PendingApprovalList {
    member: PendingApproval
}

structure PendingApproval {
    @required
    taskId: String

    @required
    requestId: String

    @required
    requestType: String

    @required
    submittedBy: String

    @required
    submittedAt: Long

    @required
    requiresComment: Boolean
}

/// Approve a request.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/approvals/tasks/{taskId}/approve")
operation ApproveRequest {
    input: ApproveRequestInput
    output: ApproveRequestOutput
    errors: [
        Unauthorized
        TaskNotFound
        InvalidState
        CommentRequired
    ]
}

structure ApproveRequestInput {
    @required
    @httpLabel
    taskId: String

    comment: String
}

structure ApproveRequestOutput {
    @required
    approved: Boolean

    @required
    approvedAt: Long
}

/// Reject a request.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/approvals/tasks/{taskId}/reject")
operation RejectRequest {
    input: RejectRequestInput
    output: RejectRequestOutput
    errors: [
        Unauthorized
        TaskNotFound
        InvalidState
        CommentRequired
    ]
}

structure RejectRequestInput {
    @required
    @httpLabel
    taskId: String

    @required
    reason: String
}

structure RejectRequestOutput {
    @required
    rejected: Boolean

    @required
    rejectedAt: Long
}

/// Get approval request status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/approvals/requests/{requestId}")
operation GetApprovalStatus {
    input: GetApprovalStatusInput
    output: GetApprovalStatusOutput
    errors: [
        Unauthorized
        RequestNotFound
    ]
}

structure GetApprovalStatusInput {
    @required
    @httpLabel
    requestId: String
}

structure GetApprovalStatusOutput {
    @required
    request: ApprovalRequest

    @required
    tasks: ApprovalTaskList
}

structure ApprovalRequest {
    @required
    requestId: String

    @required
    requestType: String

    @required
    submittedBy: String

    @required
    submittedAt: Long

    @required
    status: String

    approvedAt: Long

    rejectedAt: Long
}

list ApprovalTaskList {
    member: ApprovalTask
}

structure ApprovalTask {
    @required
    taskId: String

    @required
    approverId: String

    @required
    approverName: String

    @required
    status: String

    @required
    order: Integer

    actionedAt: Long

    comment: String
}

/// Delegate approval authority.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/approvals/delegate")
operation DelegateApprovalAuthority {
    input: DelegateApprovalAuthorityInput
    output: DelegateApprovalAuthorityOutput
    errors: [
        Unauthorized
        InvalidDelegation
        EmployeeNotFound
    ]
}

structure DelegateApprovalAuthorityInput {
    @required
    delegateToId: String

    @required
    startDate: Long

    @required
    endDate: Long

    requestTypes: StringList
}

structure DelegateApprovalAuthorityOutput {
    @required
    delegationId: String

    @required
    createdAt: Long
}

/// Get approval history.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["approvals:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/approvals/history")
operation GetApprovalHistory {
    input: GetApprovalHistoryInput
    output: GetApprovalHistoryOutput
    errors: [
        Unauthorized
    ]
}

structure GetApprovalHistoryInput {
    @httpQuery("requestType")
    requestType: String

    @httpQuery("startDate")
    startDate: Long

    @httpQuery("endDate")
    endDate: Long

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetApprovalHistoryOutput {
    @required
    requests: HistoryList

    @required
    totalCount: Integer
}

list HistoryList {
    member: ApprovalHistoryRecord
}

structure ApprovalHistoryRecord {
    @required
    requestId: String

    @required
    requestType: String

    @required
    submittedBy: String

    @required
    submittedAt: Long

    @required
    finalStatus: String

    @required
    completedAt: Long
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidWorkflow {
    @required
    message: String
}

@error("client")
structure InvalidRequest {
    @required
    message: String
}

@error("client")
structure WorkflowNotFound {
    @required
    message: String
}

@error("client")
structure TaskNotFound {
    @required
    message: String
}

@error("client")
structure InvalidState {
    @required
    message: String
}

@error("client")
structure CommentRequired {
    @required
    message: String
}

@error("client")
structure RequestNotFound {
    @required
    message: String
}

@error("client")
structure InvalidDelegation {
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
