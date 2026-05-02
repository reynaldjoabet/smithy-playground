$version: "2.0"
namespace unison.residency

/// Marks a resource or operation as subject to data residency rules.
/// Data must be stored/processed in the specified region(s).
@trait(selector: "resource")
structure dataResidency {
    /// Allowed regions for storage (e.g., ["ca-central-1", "eu-west-1"])
    allowedRegions: RegionList

    /// Whether the data is classified as PII
    containsPII: Boolean = false

    /// Regulatory framework (e.g., GDPR, PIPEDA, SOX)
    complianceFramework: String
}

list RegionList {
    member: String
}