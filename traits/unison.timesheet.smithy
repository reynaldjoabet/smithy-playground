$version: "2.0"
namespace unison.timesheet
use unison#tenantScoped
use unison.operations#auditable
use unison.auth#requiredPermissions
use unison.temporal#payPeriodBound
@payPeriodBound(
    periodResolver: "INFERRED_FROM_EFFECTIVE_DATE"
    correctionWindowHours: 48
    lockBypassRoles: ["PAYROLL_MANAGER", "SYSTEM_ADMIN"]
)
@tenantScoped
@auditable(sensitivity: "HIGH", retentionDays: 2555)
@requiredPermissions(scopes: ["time:write"], minimumRole: "MANAGER")
@idempotent
@http(method: "PUT", uri: "/employees/{employeeId}/timesheets/{date}")
operation UpdateTimesheetEntry {
    input: UpdateTimesheetEntryInput
    output: UpdateTimesheetEntryOutput
    errors: [
        PayPeriodLocked
        CorrectionWindowExpired
        EmployeeNotFound
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure UpdateTimesheetEntryInput {
    @httpLabel
    @required
    employeeId: String
    @httpLabel
    @required
    date: String
}

structure UpdateTimesheetEntryOutput {
    success: Boolean
}

@error("client")
structure PayPeriodLocked {
    message: String
}

@error("client")
structure CorrectionWindowExpired {
    message: String
}

@error("client")
structure EmployeeNotFound {
    message: String
}

// Code generators produce middleware that automatically rejects writes to locked periods unless the caller has a bypass role and is within the correction window.