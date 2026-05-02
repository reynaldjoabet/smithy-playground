$version: "2.0"

namespace unison.payroll.operations

use unison.identity#authenticationRequired

// ============================================================================
// PAYROLL PROCESSING OPERATIONS - Pay Runs, Calculations, Disbursement
// ============================================================================
/// Create a new pay run.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["payroll:write", "hr:admin"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/payroll/runs")
operation CreatePayRun {
    input: CreatePayRunInput
    output: CreatePayRunOutput
    errors: [
        Unauthorized
        InvalidPayPeriod
        PayRunExists
        InsufficientData
    ]
}

structure CreatePayRunInput {
    @required
    payPeriodStart: Long

    @required
    payPeriodEnd: Long

    @required
    paymentDate: Long

    @required
    paymentMethod: String

    description: String
}

structure CreatePayRunOutput {
    @required
    payRunId: String

    @required
    createdAt: Long

    @required
    status: String
}

/// Get pay run details with employee summaries.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["payroll:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/payroll/runs/{payRunId}")
operation GetPayRunDetails {
    input: GetPayRunDetailsInput
    output: GetPayRunDetailsOutput
    errors: [
        Unauthorized
        PayRunNotFound
    ]
}

structure GetPayRunDetailsInput {
    @required
    @httpLabel
    payRunId: String
}

structure GetPayRunDetailsOutput {
    @required
    payRun: PayRun

    @required
    employeePayrolls: EmployeePayrollList

    @required
    summary: PayRunSummary
}

structure PayRun {
    @required
    payRunId: String

    @required
    payPeriodStart: Long

    @required
    payPeriodEnd: Long

    @required
    paymentDate: Long

    @required
    status: String

    @required
    createdAt: Long
}

list EmployeePayrollList {
    member: EmployeePayroll
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure EmployeePayroll {
    @required
    employeeId: String

    @required
    employeeName: String

    @required
    grossPay: Double

    @required
    totalDeductions: Double

    @required
    netPay: Double

    @required
    paymentStatus: String
}

structure PayRunSummary {
    @required
    totalEmployees: Integer

    @required
    totalGrossPay: Double

    @required
    totalDeductions: Double

    @required
    totalNetPay: Double

    @required
    totalTaxes: Double
}

/// List all pay runs with filtering.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["payroll:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/payroll/runs")
operation ListPayRuns {
    input: ListPayRunsInput
    output: ListPayRunsOutput
    errors: [
        Unauthorized
        InvalidFilter
    ]
}

structure ListPayRunsInput {
    @httpQuery("status")
    status: String

    @httpQuery("startDate")
    startDate: Long

    @httpQuery("endDate")
    endDate: Long

    @httpQuery("limit")
    limit: Integer = 50

    @httpQuery("offset")
    offset: Integer = 0
}

structure ListPayRunsOutput {
    @required
    payRuns: PayRunList

    @required
    totalCount: Integer
}

list PayRunList {
    member: PayRun
}

/// Calculate payroll for an employee.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["payroll:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/payroll/calculate")
operation CalculateEmployeePayroll {
    input: CalculateEmployeePayrollInput
    output: CalculateEmployeePayrollOutput
    errors: [
        Unauthorized
        EmployeeNotFound
        MissingData
        InvalidConfiguration
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure CalculateEmployeePayrollInput {
    @required
    employeeId: String

    @required
    payPeriodStart: Long

    @required
    payPeriodEnd: Long

    hoursWorked: Double

    overtimeHours: Double

    bonusAmount: Double

    additionalDeductions: DeductionList
}

list DeductionList {
    member: Deduction
}

structure Deduction {
    @required
    name: String

    @required
    amount: Double
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure CalculateEmployeePayrollOutput {
    @required
    employeeId: String

    @required
    grossPay: Double

    @required
    baseSalary: Double

    @required
    overtime: Double

    @required
    bonus: Double

    @required
    deductions: CalculatedDeductionList

    @required
    taxes: TaxBreakdown

    @required
    netPay: Double
}

list CalculatedDeductionList {
    member: CalculatedDeduction
}

structure CalculatedDeduction {
    @required
    name: String

    @required
    amount: Double

    @required
    type: String
}

structure TaxBreakdown {
    @required
    federalIncomeTax: Double

    @required
    stateIncomeTax: Double

    @required
    socialSecurity: Double

    @required
    medicare: Double

    @required
    totalTax: Double
}

/// Approve pay run for processing.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["payroll:approve"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/payroll/runs/{payRunId}/approve")
operation ApprovePayRun {
    input: ApprovePayRunInput
    output: ApprovePayRunOutput
    errors: [
        Unauthorized
        PayRunNotFound
        InvalidState
    ]
}

structure ApprovePayRunInput {
    @required
    @httpLabel
    payRunId: String

    approvalNotes: String
}

structure ApprovePayRunOutput {
    @required
    approved: Boolean

    @required
    approvedAt: Long
}

/// Process payment (disburse funds).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: {
        required: true
        allowedMethods: ["TOTP"]
        forceStepUp: true
    }
    ssoRequired: false
    requiredScopes: ["payroll:process"]
    sessionAffinity: "STICKY_SESSION"
    maxTokenAgeSec: 900
)
@http(method: "POST", uri: "/payroll/runs/{payRunId}/process")
operation ProcessPayment {
    input: ProcessPaymentInput
    output: ProcessPaymentOutput
    errors: [
        Unauthorized
        PayRunNotFound
        AlreadyProcessed
        InsufficientFunds
    ]
}

structure ProcessPaymentInput {
    @required
    @httpLabel
    payRunId: String

    bankingDetails: String
}

structure ProcessPaymentOutput {
    @required
    processed: Boolean

    @required
    processedAt: Long

    @required
    transactionId: String
}

/// Get employee payslip.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["payroll:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/payroll/payslips/{paySlipId}")
operation GetPaySlip {
    input: GetPaySlipInput
    output: GetPaySlipOutput
    errors: [
        Unauthorized
        PaySlipNotFound
    ]
}

structure GetPaySlipInput {
    @required
    @httpLabel
    paySlipId: String
}

structure GetPaySlipOutput {
    @required
    paySlip: PaySlip
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure PaySlip {
    @required
    paySlipId: String

    @required
    employeeId: String

    @required
    employeeName: String

    @required
    payPeriodStart: Long

    @required
    payPeriodEnd: Long

    @required
    grossPay: Double

    @required
    deductions: CalculatedDeductionList

    @required
    netPay: Double

    @required
    generatedAt: Long
}

/// Get year-to-date payroll summary.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["payroll:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/payroll/ytd-summary")
operation GetYTDSummary {
    input: GetYTDSummaryInput
    output: GetYTDSummaryOutput
    errors: [
        Unauthorized
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure GetYTDSummaryInput {
    @httpQuery("year")
    year: Integer

    @httpQuery("employeeId")
    employeeId: String
}

structure GetYTDSummaryOutput {
    @required
    totalGrossPay: Double

    @required
    totalDeductions: Double

    @required
    totalNetPay: Double

    @required
    totalTaxesPaid: Double

    @required
    payPeriods: Integer
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure InvalidPayPeriod {
    @required
    message: String
}

@error("client")
structure PayRunExists {
    @required
    message: String

    existingPayRunId: String
}

@error("client")
structure InsufficientData {
    @required
    message: String

    missingFields: StringList
}

@error("client")
structure PayRunNotFound {
    @required
    message: String
}

@error("client")
structure InvalidFilter {
    @required
    message: String
}

@error("client")
structure EmployeeNotFound {
    @required
    message: String
}

@error("client")
structure MissingData {
    @required
    message: String

    missingFields: StringList
}

@error("client")
structure InvalidConfiguration {
    @required
    message: String
}

@error("client")
structure InvalidState {
    @required
    message: String
}

@error("client")
structure AlreadyProcessed {
    @required
    message: String
}

@error("server")
structure InsufficientFunds {
    @required
    message: String
}

@error("client")
structure PaySlipNotFound {
    @required
    message: String
}

list StringList {
    member: String
}
