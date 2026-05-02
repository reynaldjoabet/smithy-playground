$version: "2.0"

namespace unison.notifications.operations

use unison.identity#authenticationRequired

// ============================================================================
// NOTIFICATIONS & ALERTS OPERATIONS - User Notifications, Email
// ============================================================================
/// Get user notification preferences.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/notifications/preferences")
operation GetNotificationPreferences {
    input: GetNotificationPreferencesInput
    output: GetNotificationPreferencesOutput
    errors: [
        Unauthorized
    ]
}

structure GetNotificationPreferencesInput {}

structure GetNotificationPreferencesOutput {
    @required
    preferences: PreferenceList
}

list PreferenceList {
    member: NotificationPreference
}

structure NotificationPreference {
    @required
    type: String

    @required
    enabled: Boolean

    @required
    channels: ChannelList
}

list ChannelList {
    member: Channel
}

structure Channel {
    @required
    type: String

    @required
    enabled: Boolean
}

/// Update notification preferences.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "PATCH", uri: "/notifications/preferences")
operation UpdateNotificationPreferences {
    input: UpdateNotificationPreferencesInput
    output: UpdateNotificationPreferencesOutput
    errors: [
        Unauthorized
        InvalidPreferences
    ]
}

structure UpdateNotificationPreferencesInput {
    @required
    preferences: PreferenceList
}

structure UpdateNotificationPreferencesOutput {
    @required
    updated: Boolean

    @required
    updatedAt: Long
}

/// Get unread notifications.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/notifications/unread")
operation GetUnreadNotifications {
    input: GetUnreadNotificationsInput
    output: GetUnreadNotificationsOutput
    errors: [
        Unauthorized
    ]
}

structure GetUnreadNotificationsInput {
    @httpQuery("limit")
    limit: Integer = 50
}

structure GetUnreadNotificationsOutput {
    @required
    notifications: NotificationList

    @required
    unreadCount: Integer
}

list NotificationList {
    member: Notification
}

structure Notification {
    @required
    notificationId: String

    @required
    type: String

    @required
    title: String

    @required
    message: String

    @required
    createdAt: Long

    @required
    isRead: Boolean

    actionUrl: String
}

/// Mark notification as read.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/notifications/{notificationId}/read")
operation MarkNotificationAsRead {
    input: MarkNotificationAsReadInput
    output: MarkNotificationAsReadOutput
    errors: [
        Unauthorized
        NotificationNotFound
    ]
}

structure MarkNotificationAsReadInput {
    @required
    @httpLabel
    notificationId: String
}

structure MarkNotificationAsReadOutput {
    @required
    marked: Boolean

    @required
    markedAt: Long
}

/// Mark all notifications as read.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/notifications/mark-all-read")
operation MarkAllNotificationsAsRead {
    input: MarkAllNotificationsAsReadInput
    output: MarkAllNotificationsAsReadOutput
    errors: [
        Unauthorized
    ]
}

structure MarkAllNotificationsAsReadInput {}

structure MarkAllNotificationsAsReadOutput {
    @required
    markedCount: Integer

    @required
    markedAt: Long
}

/// Delete a notification.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@idempotent
@http(method: "DELETE", uri: "/notifications/{notificationId}")
operation DeleteNotification {
    input: DeleteNotificationInput
    output: DeleteNotificationOutput
    errors: [
        Unauthorized
        NotificationNotFound
    ]
}

structure DeleteNotificationInput {
    @required
    @httpLabel
    notificationId: String
}

structure DeleteNotificationOutput {
    @required
    deleted: Boolean

    @required
    deletedAt: Long
}

/// Send notification to employee.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["notifications:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/notifications/send")
operation SendNotification {
    input: SendNotificationInput
    output: SendNotificationOutput
    errors: [
        Unauthorized
        InvalidRecipient
        InvalidContent
    ]
}

structure SendNotificationInput {
    @required
    recipientId: String

    @required
    type: String

    @required
    title: String

    @required
    message: String

    channels: StringList

    urgency: String
}

structure SendNotificationOutput {
    @required
    notificationId: String

    @required
    sentAt: Long
}

/// Get notification history.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["notifications:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/notifications/history")
operation GetNotificationHistory {
    input: GetNotificationHistoryInput
    output: GetNotificationHistoryOutput
    errors: [
        Unauthorized
    ]
}

structure GetNotificationHistoryInput {
    @httpQuery("startDate")
    startDate: Long

    @httpQuery("endDate")
    endDate: Long

    @httpQuery("type")
    type: String

    @httpQuery("limit")
    limit: Integer = 100

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetNotificationHistoryOutput {
    @required
    notifications: HistoryNotificationList

    @required
    totalCount: Integer
}

list HistoryNotificationList {
    member: HistoryNotification
}

structure HistoryNotification {
    @required
    notificationId: String

    @required
    type: String

    @required
    title: String

    @required
    createdAt: Long

    @required
    isRead: Boolean

    readAt: Long
}

/// Send bulk notifications.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["notifications:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/notifications/bulk")
operation SendBulkNotification {
    input: SendBulkNotificationInput
    output: SendBulkNotificationOutput
    errors: [
        Unauthorized
        InvalidRecipients
        InvalidContent
    ]
}

structure SendBulkNotificationInput {
    @required
    recipientIds: StringList

    @required
    type: String

    @required
    title: String

    @required
    message: String

    channels: StringList
}

structure SendBulkNotificationOutput {
    @required
    jobId: String

    @required
    createdAt: Long

    recipientCount: Integer
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidPreferences {
    @required
    message: String
}

@error("client")
structure NotificationNotFound {
    @required
    message: String
}

@error("client")
structure InvalidRecipient {
    @required
    message: String
}

@error("client")
structure InvalidContent {
    @required
    message: String
}

@error("client")
structure InvalidRecipients {
    @required
    message: String

    failedCount: Integer
}

list StringList {
    member: String
}
