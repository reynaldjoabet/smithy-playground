$version: "2.0"
namespace unison.jurisdiction
use unison.temporal#payPeriodBound
use unison.operations#auditable
@jurisdictionAware(
    resolution: "FROM_LEGAL_ENTITY"
    validationRuleSets: {
        "CA-FED": {
            rules: ["T4_VALIDATION", "CPP2_THRESHOLD_2026", "EI_MAX_INSURABLE"]
            enforcementMode: "STRICT"
        }
        "CA-QC": {
            rules: ["RL1_VALIDATION", "QPP_THRESHOLD_2026", "QPIP_VALIDATION"]
            enforcementMode: "STRICT"
        }
        "US-FED": {
            rules: ["W2_VALIDATION", "FICA_LIMITS", "FUTA_THRESHOLD"]
            enforcementMode: "STRICT"
        }
        "DE": {
            rules: ["LOHNSTEUER_VALIDATION", "SOZIALVERSICHERUNG_LIMITS"]
            enforcementMode: "WARN_AND_PROCEED"
        }
    }
    triggersStatutoryReporting: true
)
@payPeriodBound(
    periodResolver: "FROM_REQUEST_BODY"
    correctionWindowHours: 0
    lockBypassRoles: ["SYSTEM_ADMIN"]
)
@auditable(sensitivity: "CRITICAL", retentionDays: 2555)
@http(method: "POST", uri: "/legal-entities/{legalEntityId}/tax-filings")
operation SubmitTaxFiling {
    input: SubmitTaxFilingInput
    output: SubmitTaxFilingOutput
    errors: [
        JurisdictionValidationFailed
        UnsupportedJurisdiction
        PayPeriodLocked
        MissingStatutoryFields
    ]
}

structure SubmitTaxFilingInput {
    @httpLabel
    @required
    legalEntityId: String
}

structure SubmitTaxFilingOutput {}

@error("client")
structure JurisdictionValidationFailed {}

@error("client")
structure UnsupportedJurisdiction {}

@error("client")
structure PayPeriodLocked {}

@error("client")
structure MissingStatutoryFields {}

// A compliance auditor can now read the model and confirm: "Yes, Quebec payroll uses RL-1 rules and QPP thresholds, separate from federal T4/CPP."