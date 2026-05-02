$version: "2.0"
namespace unison.operations
use unison#tenantScoped
use unison.operations#auditable
use unison.auth#requiredPermissions
use unison.jurisdiction#jurisdictionAware
use unison.events#emitsEvents

@emitsEvents(events: [
    {
        eventType: "unison.hr.employee.terminated"
        destination: "sns://unison-employee-lifecycle"
        knownConsumers: [
            "benefits-service:terminate-coverage"
            "payroll-service:final-pay-calculation"
            "gl-service:accrual-reversal"
            "it-service:deprovision-access"
            "reporting-service:headcount-update"
        ]
        deliveryGuarantee: "EXACTLY_ONCE"
        slaMs: 30000
    }
    {
        eventType: "unison.compliance.statutory-termination-record"
        destination: "sqs://compliance-statutory-queue"
        knownConsumers: [
            "compliance-service:roe-generation"
            "compliance-service:cobra-notification"
        ]
        deliveryGuarantee: "EXACTLY_ONCE"
        slaMs: 60000
    }
])
@tenantScoped
@auditable(sensitivity: "CRITICAL", retentionDays: 2555)
@requiredPermissions(scopes: ["employee:write", "hr:admin"], minimumRole: "HR_ADMIN")
@jurisdictionAware(
    resolution: "FROM_EMPLOYEE_WORK_LOCATION"
    validationRuleSets: {
        "CA-FED": {
            rules: ["ROE_REQUIRED", "MINIMUM_NOTICE_PERIOD"]
            enforcementMode: "STRICT"
        }
        "US-FED": {
            rules: ["COBRA_ELIGIBILITY", "WARN_ACT_CHECK"]
            enforcementMode: "STRICT"
        }
    }
    triggersStatutoryReporting: true
)
@http(method: "POST", uri: "/employees/{employeeId}/terminate")
operation TerminateEmployee {
    input: TerminateEmployeeInput
    output: TerminateEmployeeOutput
    errors: [
        EmployeeNotFound
        TerminationBlockedByActivePayRun
        JurisdictionValidationFailed
        MinimumNoticePeriodViolation
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure TerminateEmployeeInput {
    @required
    @httpLabel
    employeeId: String
}

structure TerminateEmployeeOutput {
    terminationId: String
}

@error("client")
structure EmployeeNotFound {
    message: String
}

@error("client")
structure TerminationBlockedByActivePayRun {
    message: String
}

@error("client")
structure JurisdictionValidationFailed {
    message: String
}

@error("client")
structure MinimumNoticePeriodViolation {
    message: String
}
