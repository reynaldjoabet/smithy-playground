$version: "2.0"

namespace unison.bulkoperations.operations

use unison.identity#authenticationRequired

// ============================================================================
// BULK OPERATIONS - Data Import/Export, Migrations, Batch Updates
// ============================================================================
/// Import employee data from file.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["bulkoperations:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/bulkoperations/import")
operation ImportEmployeeData {
    input: ImportEmployeeDataInput
    output: ImportEmployeeDataOutput
    errors: [
        Unauthorized
        InvalidFile
        ParseError
        ValidationFailed
    ]
}

structure ImportEmployeeDataInput {
    @required
    dataType: String

    @required
    fileContent: String

    @required
    format: String

    updateExisting: Boolean = false

    sendWelcomeEmail: Boolean = true
}

structure ImportEmployeeDataOutput {
    @required
    jobId: String

    @required
    createdAt: Long

    @required
    estimatedProcessingTime: Integer
}

/// Get import job status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["bulkoperations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/bulkoperations/import-jobs/{jobId}")
operation GetImportJobStatus {
    input: GetImportJobStatusInput
    output: GetImportJobStatusOutput
    errors: [
        Unauthorized
        JobNotFound
    ]
}

structure GetImportJobStatusInput {
    @required
    @httpLabel
    jobId: String
}

structure GetImportJobStatusOutput {
    @required
    job: ImportJob
}

structure ImportJob {
    @required
    jobId: String

    @required
    dataType: String

    @required
    status: String

    @required
    createdAt: Long

    @required
    recordsProcessed: Integer

    recordsSuccessful: Integer

    recordsFailed: Integer

    completedAt: Long

    errors: ErrorDetailList
}

list ErrorDetailList {
    member: ErrorDetail
}

structure ErrorDetail {
    @required
    rowNumber: Integer

    @required
    errorMessage: String

    @required
    errorCode: String
}

/// Get import error report.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["bulkoperations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/bulkoperations/import-jobs/{jobId}/errors")
operation GetImportErrorReport {
    input: GetImportErrorReportInput
    output: GetImportErrorReportOutput
    errors: [
        Unauthorized
        JobNotFound
        ReportNotReady
    ]
}

structure GetImportErrorReportInput {
    @required
    @httpLabel
    jobId: String
}

structure GetImportErrorReportOutput {
    @required
    downloadUrl: String

    @required
    expiresAt: Long
}

/// Batch update employees.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["bulkoperations:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/bulkoperations/batch-update")
operation BatchUpdateEmployees {
    input: BatchUpdateEmployeesInput
    output: BatchUpdateEmployeesOutput
    errors: [
        Unauthorized
        InvalidUpdates
        NoUpdates
    ]
}

structure BatchUpdateEmployeesInput {
    @required
    updates: UpdateList

    sendNotifications: Boolean = true
}

list UpdateList {
    member: EmployeeUpdate
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure EmployeeUpdate {
    @required
    employeeId: String

    @required
    updates: StringMap
}

structure StringMap {
    field: String
}

structure BatchUpdateEmployeesOutput {
    @required
    jobId: String

    @required
    createdAt: Long

    totalUpdates: Integer
}

/// Get batch update status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["bulkoperations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/bulkoperations/batch-update-jobs/{jobId}")
operation GetBatchUpdateStatus {
    input: GetBatchUpdateStatusInput
    output: GetBatchUpdateStatusOutput
    errors: [
        Unauthorized
        JobNotFound
    ]
}

structure GetBatchUpdateStatusInput {
    @required
    @httpLabel
    jobId: String
}

structure GetBatchUpdateStatusOutput {
    @required
    job: BatchUpdateJob
}

structure BatchUpdateJob {
    @required
    jobId: String

    @required
    status: String

    @required
    createdAt: Long

    @required
    totalRecords: Integer

    processedRecords: Integer

    succeededRecords: Integer

    failedRecords: Integer

    completedAt: Long
}

/// Export all employee data.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["bulkoperations:read", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/bulkoperations/export")
operation ExportAllEmployeeData {
    input: ExportAllEmployeeDataInput
    output: ExportAllEmployeeDataOutput
    errors: [
        Unauthorized
        InvalidFormat
    ]
}

structure ExportAllEmployeeDataInput {
    @required
    format: String

    includeInactive: Boolean = false

    includeArchived: Boolean = false

    fields: StringList
}

structure ExportAllEmployeeDataOutput {
    @required
    jobId: String

    @required
    createdAt: Long
}

/// Get export job status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["bulkoperations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/bulkoperations/export-jobs/{jobId}")
operation GetExportJobStatus {
    input: GetExportJobStatusInput
    output: GetExportJobStatusOutput
    errors: [
        Unauthorized
        JobNotFound
    ]
}

structure GetExportJobStatusInput {
    @required
    @httpLabel
    jobId: String
}

structure GetExportJobStatusOutput {
    @required
    job: ExportJob
}

structure ExportJob {
    @required
    jobId: String

    @required
    format: String

    @required
    status: String

    @required
    createdAt: Long

    completedAt: Long

    downloadUrl: String

    expiresAt: Long
}

/// Migrate employee data from legacy system.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["bulkoperations:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/bulkoperations/migrate")
operation MigrateEmployeeData {
    input: MigrateEmployeeDataInput
    output: MigrateEmployeeDataOutput
    errors: [
        Unauthorized
        InvalidMigration
        DataIntegrityError
    ]
}

structure MigrateEmployeeDataInput {
    @required
    sourceSystem: String

    @required
    dataSet: String

    mappingRules: String

    validateOnly: Boolean = false
}

structure MigrateEmployeeDataOutput {
    @required
    jobId: String

    @required
    createdAt: Long
}

/// Get migration job status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["bulkoperations:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/bulkoperations/migration-jobs/{jobId}")
operation GetMigrationJobStatus {
    input: GetMigrationJobStatusInput
    output: GetMigrationJobStatusOutput
    errors: [
        Unauthorized
        JobNotFound
    ]
}

structure GetMigrationJobStatusInput {
    @required
    @httpLabel
    jobId: String
}

structure GetMigrationJobStatusOutput {
    @required
    job: MigrationJob
}

structure MigrationJob {
    @required
    jobId: String

    @required
    sourceSystem: String

    @required
    status: String

    @required
    createdAt: Long

    @required
    recordsMigrated: Integer

    recordsSkipped: Integer

    recordsFailed: Integer

    completedAt: Long
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidFile {
    @required
    message: String
}

@error("client")
structure ParseError {
    @required
    message: String

    line: Integer
}

@error("client")
structure ValidationFailed {
    @required
    message: String

    errors: StringList
}

@error("client")
structure JobNotFound {
    @required
    message: String
}

@error("server")
structure ReportNotReady {
    @required
    message: String

    estimatedSeconds: Integer
}

@error("client")
structure InvalidUpdates {
    @required
    message: String
}

@error("client")
structure NoUpdates {
    @required
    message: String
}

@error("client")
structure InvalidFormat {
    @required
    message: String
}

@error("client")
structure InvalidMigration {
    @required
    message: String
}

@error("client")
structure DataIntegrityError {
    @required
    message: String

    affectedRecords: Integer
}

list StringList {
    member: String
}
