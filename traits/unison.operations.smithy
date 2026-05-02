$version: "2.0"
namespace unison.operations

/// Marks an operation as requiring audit trail logging.
@trait(selector: "operation")
structure auditable {
    /// Classification level for the audit log
    sensitivity: Sensitivity

    /// Retention period in days
    retentionDays: Integer = 365
}

enum Sensitivity {
    LOW
    MEDIUM
    HIGH
    CRITICAL
}