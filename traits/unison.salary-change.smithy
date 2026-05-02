$version: "2.0"
namespace unison.salaryChange
use unison#tenantScoped
use unison.operations#auditable
use unison.auth#requiredPermissions
use unison.temporal#payPeriodBound
use unison.jurisdiction#jurisdictionAware
use unison.continuous#calculationTrigger

@calculationTrigger(
    affectedDomains: [
        "GROSS_PAY"
        "NET_PAY"
        "STATUTORY_DEDUCTIONS"
        "EMPLOYER_CONTRIBUTIONS"
        "GL_ENTRIES"
        "YEAR_TO_DATE_ACCUMULATORS"
        "PENSION_CONTRIBUTIONS"
    ]
    recalculationScope: {
        retroactivePeriods: 0
        prospectivePeriods: 26
        cascadeToDependents: false
    }
    propagation: "TOPOLOGICAL_SUBGRAPH"
    executionMode: "HYBRID_CRITICAL_PATH"
    convergenceSlaMs: 5000
)
@tenantScoped
@auditable(sensitivity: "HIGH", retentionDays: 2555)
@requiredPermissions(scopes: ["compensation:write"], minimumRole: "HR_ADMIN")
@jurisdictionAware(
    resolution: "FROM_EMPLOYEE_WORK_LOCATION"
    validationRuleSets: {
        "CA-FED": {
            rules: ["CPP2_RECALC", "EI_PREMIUM_RECALC"]
            enforcementMode: "STRICT"
        }
        "US-FED": {
            rules: ["FICA_WAGE_BASE_CHECK", "401K_LIMIT_RECALC"]
            enforcementMode: "STRICT"
        }
    }
    triggersStatutoryReporting: false
)
@idempotent
@http(method: "PUT", uri: "/employees/{employeeId}/compensation")
operation UpdateEmployeeCompensation {
    input: UpdateEmployeeCompensationInput
    output: UpdateEmployeeCompensationOutput
    errors: [
        EmployeeNotFound
        CompensationEffectiveDateConflict
        CalculationGraphCycleDetected
        ConvergenceTimeoutExceeded
    ]
}
@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure UpdateEmployeeCompensationInput {
    @required
    @httpLabel
    employeeId: String
    @required
    salary: BigDecimal
    effectiveDate: String
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure UpdateEmployeeCompensationOutput {
    employeeId: String
    salary: BigDecimal
    effectiveDate: String
}

@error("client")
structure EmployeeNotFound {
    @required
    message: String
}

@error("client")
structure CompensationEffectiveDateConflict {
    @required
    message: String
}

@error("client")
structure CalculationGraphCycleDetected {
    @required
    message: String
}

@error("server")
structure ConvergenceTimeoutExceeded {
    @required
    message: String
}

