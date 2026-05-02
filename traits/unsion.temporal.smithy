$version: "2.0"
namespace unison.temporal
/// Enforces that mutations are only allowed within an open pay period.
/// After the period is locked, writes are rejected at the middleware layer.
@trait(selector: "operation")
structure payPeriodBound {
    /// How the pay period is resolved from the request
    periodResolver: PeriodResolver

    /// Grace window (in hours) after period close where corrections are allowed
    correctionWindowHours: Integer = 0

    /// Roles that can bypass the lock (e.g., for retroactive adjustments)
    lockBypassRoles: RoleList
}

enum PeriodResolver {
    FROM_REQUEST_BODY = "FROM_REQUEST_BODY"
    FROM_PATH_PARAM = "FROM_PATH_PARAM"
    INFERRED_FROM_EFFECTIVE_DATE = "INFERRED_FROM_EFFECTIVE_DATE"
}

list RoleList {
    member: String
}