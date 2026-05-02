$version: "2.0"
namespace unison.auth


/// Defines the required permission scopes for an operation.
/// Used by the authorization middleware to enforce RBAC.
@trait(selector: "operation")
structure requiredPermissions {
    /// The permission scopes needed (e.g., ["payroll:read", "employee:read"])
    scopes: ScopeList

    /// Minimum  role (e.g., "HR_ADMIN", "PAYROLL_MANAGER", "EMPLOYEE")
    minimumRole: String

    /// Whether the caller must also be the resource owner (self-service)
    requireSelfOwnership: Boolean = false
}

list ScopeList {
    member: String
}