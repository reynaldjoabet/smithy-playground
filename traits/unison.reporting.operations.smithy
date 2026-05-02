$version: "2.0"

namespace unison.reporting.operations

use unison.identity#authenticationRequired

// ============================================================================
// REPORTING & ANALYTICS OPERATIONS - Reports, Data Export, Insights
// ============================================================================
/// Create a custom report.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/reporting/reports")
operation CreateCustomReport {
    input: CreateCustomReportInput
    output: CreateCustomReportOutput
    errors: [
        Unauthorized
        InvalidConfiguration
        NameAlreadyExists
    ]
}

structure CreateCustomReportInput {
    @required
    reportName: String

    @required
    dataSource: String

    @required
    fields: StringList

    @required
    filters: FilterList

    schedule: String

    format: String
}

list FilterList {
    member: Filter
}

structure Filter {
    @required
    field: String

    @required
    operator: String

    @required
    value: String
}

structure CreateCustomReportOutput {
    @required
    reportId: String

    @required
    createdAt: Long
}

/// Generate report data.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@http(method: "POST", uri: "/reporting/reports/{reportId}/generate")
operation GenerateReport {
    input: GenerateReportInput
    output: GenerateReportOutput
    errors: [
        Unauthorized
        ReportNotFound
        GenerationFailed
    ]
}

structure GenerateReportInput {
    @required
    @httpLabel
    reportId: String

    @required
    format: String

    dateRange: String
}

structure GenerateReportOutput {
    @required
    jobId: String

    @required
    status: String

    @required
    createdAt: Long

    estimatedCompletionTime: Integer
}

/// Get report generation status.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/reporting/jobs/{jobId}")
operation GetReportJobStatus {
    input: GetReportJobStatusInput
    output: GetReportJobStatusOutput
    errors: [
        Unauthorized
        JobNotFound
    ]
}

structure GetReportJobStatusInput {
    @required
    @httpLabel
    jobId: String
}

structure GetReportJobStatusOutput {
    @required
    job: ReportJob
}

structure ReportJob {
    @required
    jobId: String

    @required
    reportId: String

    @required
    status: String

    @required
    createdAt: Long

    completedAt: Long

    downloadUrl: String
}

/// Download generated report.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/reporting/jobs/{jobId}/download")
operation DownloadReport {
    input: DownloadReportInput
    output: DownloadReportOutput
    errors: [
        Unauthorized
        JobNotFound
        NotReady
    ]
}

structure DownloadReportInput {
    @required
    @httpLabel
    jobId: String
}

structure DownloadReportOutput {
    @required
    fileUrl: String

    @required
    expiresAt: Long
}

/// Export employee data.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:write", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/reporting/exports")
operation ExportEmployeeData {
    input: ExportEmployeeDataInput
    output: ExportEmployeeDataOutput
    errors: [
        Unauthorized
        InvalidFields
    ]
}

structure ExportEmployeeDataInput {
    @required
    dataType: String

    @required
    format: String

    fields: StringList

    includeInactive: Boolean = false
}

structure ExportEmployeeDataOutput {
    @required
    exportId: String

    @required
    createdAt: Long
}

/// Get dashboard metrics.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/reporting/metrics")
operation GetDashboardMetrics {
    input: GetDashboardMetricsInput
    output: GetDashboardMetricsOutput
    errors: [
        Unauthorized
    ]
}

structure GetDashboardMetricsInput {
    @httpQuery("timeframe")
    timeframe: String = "month"
}

structure GetDashboardMetricsOutput {
    @required
    metrics: MetricList
}

list MetricList {
    member: Metric
}

structure Metric {
    @required
    name: String

    @required
    category: String

    @required
    value: Double

    @required
    trend: String

    previousValue: Double

    unit: String
}

/// Get trend analysis.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/reporting/trends")
operation GetTrendAnalysis {
    input: GetTrendAnalysisInput
    output: GetTrendAnalysisOutput
    errors: [
        Unauthorized
        InvalidMetric
    ]
}

structure GetTrendAnalysisInput {
    @httpQuery("metric")
    metric: String

    @httpQuery("periodMonths")
    periodMonths: Integer = 12
}

structure GetTrendAnalysisOutput {
    @required
    metric: String

    @required
    dataPoints: DataPointList
}

list DataPointList {
    member: DataPoint
}

structure DataPoint {
    @required
    date: Long

    @required
    value: Double
}

/// List predefined reports.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["reporting:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/reporting/templates")
operation ListReportTemplates {
    input: ListReportTemplatesInput
    output: ListReportTemplatesOutput
    errors: [
        Unauthorized
    ]
}

structure ListReportTemplatesInput {
    @httpQuery("category")
    category: String
}

structure ListReportTemplatesOutput {
    @required
    templates: ReportTemplateList
}

list ReportTemplateList {
    member: ReportTemplate
}

structure ReportTemplate {
    @required
    templateId: String

    @required
    name: String

    @required
    category: String

    @required
    description: String

    availableFormats: StringList
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidConfiguration {
    @required
    message: String
}

@error("client")
structure NameAlreadyExists {
    @required
    message: String
}

@error("client")
structure ReportNotFound {
    @required
    message: String
}

@error("server")
structure GenerationFailed {
    @required
    message: String

    retryAfterSeconds: Integer
}

@error("client")
structure JobNotFound {
    @required
    message: String
}

@error("client")
structure NotReady {
    @required
    message: String

    estimatedSeconds: Integer
}

@error("client")
structure InvalidFields {
    @required
    message: String
}

@error("client")
structure InvalidMetric {
    @required
    message: String
}

list StringList {
    member: String
}
