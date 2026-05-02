$version: "2.0"

namespace unison.integrations.operations

use unison.identity#authenticationRequired

// ============================================================================
// INTEGRATIONS & WEBHOOKS OPERATIONS - Third-party Integrations, Events
// ============================================================================
/// Create webhook endpoint.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/integrations/webhooks")
operation CreateWebhook {
    input: CreateWebhookInput
    output: CreateWebhookOutput
    errors: [
        Unauthorized
        InvalidUrl
        DuplicateWebhook
    ]
}

structure CreateWebhookInput {
    @required
    name: String

    @required
    url: String

    @required
    events: StringList

    @required
    isActive: Boolean

    secret: String

    retryPolicy: String
}

structure CreateWebhookOutput {
    @required
    webhookId: String

    @required
    createdAt: Long

    @required
    secret: String
}

/// List webhooks.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/integrations/webhooks")
operation ListWebhooks {
    input: ListWebhooksInput
    output: ListWebhooksOutput
    errors: [
        Unauthorized
    ]
}

structure ListWebhooksInput {
    @httpQuery("active")
    active: Boolean

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure ListWebhooksOutput {
    @required
    webhooks: WebhookList

    @required
    totalCount: Integer
}

list WebhookList {
    member: Webhook
}

structure Webhook {
    @required
    webhookId: String

    @required
    name: String

    @required
    url: String

    @required
    isActive: Boolean

    @required
    createdAt: Long

    @required
    events: StringList

    lastDeliveredAt: Long

    lastDeliveryStatus: String
}

/// Update webhook.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "PATCH", uri: "/integrations/webhooks/{webhookId}")
operation UpdateWebhook {
    input: UpdateWebhookInput
    output: UpdateWebhookOutput
    errors: [
        Unauthorized
        WebhookNotFound
        InvalidUrl
    ]
}

structure UpdateWebhookInput {
    @required
    @httpLabel
    webhookId: String

    name: String

    url: String

    events: StringList

    isActive: Boolean
}

structure UpdateWebhookOutput {
    @required
    updated: Boolean

    @required
    updatedAt: Long
}

/// Delete webhook.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@idempotent
@http(method: "DELETE", uri: "/integrations/webhooks/{webhookId}")
operation DeleteWebhook {
    input: DeleteWebhookInput
    output: DeleteWebhookOutput
    errors: [
        Unauthorized
        WebhookNotFound
    ]
}

structure DeleteWebhookInput {
    @required
    @httpLabel
    webhookId: String
}

structure DeleteWebhookOutput {
    @required
    deleted: Boolean

    @required
    deletedAt: Long
}

/// Get webhook deliveries.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/integrations/webhooks/{webhookId}/deliveries")
operation GetWebhookDeliveries {
    input: GetWebhookDeliveriesInput
    output: GetWebhookDeliveriesOutput
    errors: [
        Unauthorized
        WebhookNotFound
    ]
}

structure GetWebhookDeliveriesInput {
    @required
    @httpLabel
    webhookId: String

    @httpQuery("status")
    status: String

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetWebhookDeliveriesOutput {
    @required
    deliveries: DeliveryList

    @required
    totalCount: Integer
}

list DeliveryList {
    member: Delivery
}

structure Delivery {
    @required
    deliveryId: String

    @required
    webhookId: String

    @required
    event: String

    @required
    status: String

    @required
    statusCode: Integer

    @required
    deliveredAt: Long

    payload: String

    response: String
}

/// Retry webhook delivery.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/integrations/webhooks/{webhookId}/deliveries/{deliveryId}/retry")
operation RetryWebhookDelivery {
    input: RetryWebhookDeliveryInput
    output: RetryWebhookDeliveryOutput
    errors: [
        Unauthorized
        DeliveryNotFound
        InvalidState
    ]
}

structure RetryWebhookDeliveryInput {
    @required
    @httpLabel
    webhookId: String

    @required
    @httpLabel
    deliveryId: String
}

structure RetryWebhookDeliveryOutput {
    @required
    retried: Boolean

    @required
    retriedAt: Long
}

/// Register third-party integration.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:write", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/integrations/register")
operation RegisterThirdPartyIntegration {
    input: RegisterThirdPartyIntegrationInput
    output: RegisterThirdPartyIntegrationOutput
    errors: [
        Unauthorized
        InvalidIntegration
        AlreadyRegistered
    ]
}

structure RegisterThirdPartyIntegrationInput {
    @required
    integrationType: String

    @required
    name: String

    @required
    credentials: String

    dataSync: String
}

structure RegisterThirdPartyIntegrationOutput {
    @required
    integrationId: String

    @required
    registeredAt: Long

    @required
    status: String
}

/// Get list of available integrations.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/integrations/available")
operation ListAvailableIntegrations {
    input: ListAvailableIntegrationsInput
    output: ListAvailableIntegrationsOutput
    errors: [
        Unauthorized
    ]
}

structure ListAvailableIntegrationsInput {
    @httpQuery("category")
    category: String
}

structure ListAvailableIntegrationsOutput {
    @required
    integrations: AvailableIntegrationList
}

list AvailableIntegrationList {
    member: AvailableIntegration
}

structure AvailableIntegration {
    @required
    integrationType: String

    @required
    name: String

    @required
    description: String

    @required
    category: String

    @required
    isEnabled: Boolean

    documentationUrl: String
}

/// Get registered integration details.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/integrations/{integrationId}")
operation GetIntegrationDetails {
    input: GetIntegrationDetailsInput
    output: GetIntegrationDetailsOutput
    errors: [
        Unauthorized
        IntegrationNotFound
    ]
}

structure GetIntegrationDetailsInput {
    @required
    @httpLabel
    integrationId: String
}

structure GetIntegrationDetailsOutput {
    @required
    integration: IntegrationDetails
}

structure IntegrationDetails {
    @required
    integrationId: String

    @required
    integrationType: String

    @required
    name: String

    @required
    status: String

    @required
    registeredAt: Long

    lastSyncedAt: Long

    syncErrors: ErrorList
}

list ErrorList {
    member: SyncError
}

structure SyncError {
    @required
    timestamp: Long

    @required
    errorMessage: String

    @required
    errorType: String
}

/// Trigger manual data sync.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["integrations:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/integrations/{integrationId}/sync")
operation TriggerDataSync {
    input: TriggerDataSyncInput
    output: TriggerDataSyncOutput
    errors: [
        Unauthorized
        IntegrationNotFound
        SyncInProgress
    ]
}

structure TriggerDataSyncInput {
    @required
    @httpLabel
    integrationId: String

    syncType: String
}

structure TriggerDataSyncOutput {
    @required
    jobId: String

    @required
    status: String

    @required
    createdAt: Long
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidUrl {
    @required
    message: String
}

@error("client")
structure DuplicateWebhook {
    @required
    message: String
}

@error("client")
structure WebhookNotFound {
    @required
    message: String
}

@error("client")
structure DeliveryNotFound {
    @required
    message: String
}

@error("client")
structure InvalidState {
    @required
    message: String
}

@error("client")
structure InvalidIntegration {
    @required
    message: String
}

@error("client")
structure AlreadyRegistered {
    @required
    message: String
}

@error("client")
structure IntegrationNotFound {
    @required
    message: String
}

@error("server")
structure SyncInProgress {
    @required
    message: String
}

list StringList {
    member: String
}
