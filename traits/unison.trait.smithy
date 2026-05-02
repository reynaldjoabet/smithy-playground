$version: "2.0"

namespace unison

/// Indicates this operation requires tenant-scoped access.
/// The runtime will enforce that data access is restricted
/// to the authenticated tenant's boundary.
@trait(selector: "operation")
structure tenantScoped {
    /// The header or path param carrying the tenant identifier
    tenantIdSource: String = "X-Unison-TenantId"

    /// Whether cross-tenant access is ever permitted (e.g., for superadmins)
    allowCrossTenant: Boolean = false
}