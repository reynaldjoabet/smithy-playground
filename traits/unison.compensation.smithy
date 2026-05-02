$version: "2.0"

namespace unison.compensation
use unison.auth#requiredPermissions

@requiredPermissions(
    scopes: ["compensation:read", "employee:read"]
    minimumRole: "HR_ADMIN"
)
@readonly
@http(method: "GET", uri: "/employees/{employeeId}/compensation")
operation GetEmployeeCompensation {
    input: GetEmployeeCompensationInput
    output: GetEmployeeCompensationOutput
}

@requiredPermissions(
    scopes: ["compensation:read"]
    minimumRole: "EMPLOYEE"
    requireSelfOwnership: true
)
@readonly
@http(method: "GET", uri: "/me/compensation")
operation GetMyCompensation {
    input: GetMyCompensationInput
    output: GetMyCompensationOutput
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure GetEmployeeCompensationInput {
    @httpLabel
    @required
    employeeId: String
}

structure GetEmployeeCompensationOutput {
    compensation: String
}

structure GetMyCompensationInput {}

structure GetMyCompensationOutput {
    compensation: String
}
// Notice how GetMyCompensation uses requireSelfOwnership: true — an employee can only view their own compensation, enforced at the model level.
