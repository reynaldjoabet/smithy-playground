
$version: "2.0"
namespace unison.encryption
/// Marks a member as requiring field-level encryption at rest and in transit.
/// Code generators produce serializers that automatically encrypt/decrypt
/// these fields using the tenant's KMS key.
@trait(selector: "member")
structure encryptedField {
    /// Encryption context for KMS key derivation
    encryptionContext: String

    /// Classification driving which key ring and rotation policy to use
    classification: DataClassification

    /// Whether this field should be masked in logs and API responses
    /// to non-privileged callers
    masking: MaskingStrategy
}

enum DataClassification {
    PII
    SPII                    /// Sensitive PII (SSN, SIN, etc.)
    PHI                     /// Protected Health Information
    PCI                     /// Payment Card Industry
    FINANCIAL
}

enum MaskingStrategy {
    NONE
    LAST_FOUR               /// Show only last 4 chars
    FULL_MASK               /// Replace with ****
    TOKENIZED               /// Return a reversible token
    ROLE_DEPENDENT
}