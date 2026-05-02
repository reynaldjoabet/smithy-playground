$version: "2.0"
namespace unison.operations
use unison#tenantScoped
use unison.operations#auditable
use unison.auth#requiredPermissions

@auditable(sensitivity: "CRITICAL", retentionDays: 2555)
@requiredPermissions(scopes: ["payroll:write"], minimumRole: "PAYROLL_MANAGER")
@tenantScoped
operation SubmitPayRun {
    input: SubmitPayRunInput,
    output: SubmitPayRunOutput
}

structure SubmitPayRunInput {
}

structure SubmitPayRunOutput {
}