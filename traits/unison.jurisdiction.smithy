
$version: "2.0"
namespace unison.jurisdiction

/// Declares which legal jurisdictions an operation or resource applies to,
/// and what regulatory validations must be enforced.
@trait(selector: "operation")
structure jurisdictionAware {
    /// How jurisdiction is determined from the request
    resolution: JurisdictionResolution

    /// Jurisdiction-specific validation rule sets to apply
    validationRuleSets: ValidationRuleSetMap

    /// Whether this operation triggers statutory reporting obligations
    triggersStatutoryReporting: Boolean = false
}

enum JurisdictionResolution {
    FROM_EMPLOYEE_WORK_LOCATION
    FROM_LEGAL_ENTITY
    FROM_REQUEST_HEADER
    MULTI_JURISDICTION
}

map ValidationRuleSetMap {
    key: String   /// Jurisdiction code (e.g., "CA-ON", "US-CA", "UK", "DE")
    value: JurisdictionRules
}

structure JurisdictionRules {
    /// Named rule sets to invoke (e.g., ["CA_CPP2_2024", "CA_EI_MAX"])
    rules: StringList

    /// Whether violations are hard errors or warnings
    enforcementMode: EnforcementMode
}

list StringList {
    member: String
}

enum EnforcementMode {
    STRICT
    WARN_AND_PROCEED
    AUDIT_ONLY
}