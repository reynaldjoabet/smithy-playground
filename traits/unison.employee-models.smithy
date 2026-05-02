
$version: "2.0"
namespace unison.employee
use unison.encryption#encryptedField

structure EmployeePersonalInfo {
    firstName: String
    lastName: String

    @encryptedField(
        encryptionContext: "employee-identity"
        classification: "SPII"
        masking: "LAST_FOUR"
    )
    socialInsuranceNumber: String

    @encryptedField(
        encryptionContext: "employee-identity"
        classification: "SPII"
        masking: "LAST_FOUR"
    )
    socialSecurityNumber: String

    @encryptedField(
        encryptionContext: "employee-financial"
        classification: "FINANCIAL"
        masking: "ROLE_DEPENDENT"
    )
    bankAccountNumber: String

    @encryptedField(
        encryptionContext: "employee-financial"
        classification: "PCI"
        masking: "FULL_MASK"
    )
    bankRoutingNumber: String

    @encryptedField(
        encryptionContext: "employee-health"
        classification: "PHI"
        masking: "FULL_MASK"
    )
    disabilityDetails: String

    dateOfBirth: Timestamp
    email: String
}
