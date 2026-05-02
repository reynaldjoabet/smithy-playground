$version: "2.0"
namespace unison.operations
use unison.residency#dataResidency

@dataResidency(
    allowedRegions: ["ca-central-1", "eu-west-1"]
    containsPII: true
    complianceFramework: "GDPR"
)
resource EmployeeRecord {
    identifiers: { employeeId: String }
    read: GetEmployee
    update: UpdateEmployee
}

@readonly
operation GetEmployee {
    input: GetEmployeeInput
    output: GetEmployeeOutput
}

operation UpdateEmployee {
    input: UpdateEmployeeInput
    output: UpdateEmployeeOutput
}

structure GetEmployeeInput {
    @required
    employeeId: String
}

structure GetEmployeeOutput {
    employeeId: String
}

structure UpdateEmployeeInput {
    @required
    employeeId: String
}

structure UpdateEmployeeOutput {
    employeeId: String
}

// Now compliance teams can audit the Smithy model directly to verify that employee PII is constrained to GDPR-compliant regions — no need to dig through infrastructure code.