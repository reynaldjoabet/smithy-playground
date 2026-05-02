$version: "2.0"
namespace unison.hierarchy

/// Declares how an operation interacts with the organizational hierarchy.
/// The runtime enforces visibility and data access based on the caller's
/// position in the org tree.
@trait(selector: "operation")
structure orgHierarchyScoped {
    /// Which hierarchy tree this operation is scoped to
    hierarchyType: HierarchyType

    /// How deep the caller can see below their node
    visibilityDepth: VisibilityDepth

    /// Whether the caller can act on behalf of someone in their chain
    delegationRules: DelegationConfig

    /// Whether results include inherited values from parent nodes
    inheritanceMode: InheritanceMode
}

enum HierarchyType {
    REPORTING_STRUCTURE
    COST_CENTER_HIERARCHY
    LEGAL_ENTITY_STRUCTURE
    LOCATION_HIERARCHY
    POSITION_HIERARCHY
    UNION_LOCAL_STRUCTURE
}

structure VisibilityDepth {
    /// Number of levels below the caller's node (-1 = unlimited)
    downwardLevels: Integer

    /// Whether peer nodes at the same level are visible
    includePeers: Boolean

    /// Whether upward traversal is permitted (e.g., seeing your manager's data)
    upwardLevels: Integer
}

structure DelegationConfig {
    /// Whether a manager can act on behalf of their reports
    allowManagerActsAsReport: Boolean

    /// Whether delegation chains are supported (A delegates to B delegates to C)
    allowTransitiveDelegation: Boolean

    /// Maximum delegation depth
    maxDelegationDepth: Integer

    /// Time-bounded delegation (e.g., covering for someone on leave)
    supportTemporaryDelegation: Boolean

    /// Delegatable scope restrictions — which actions can be delegated
    delegatableScopes: StringList
}

list StringList {
    member: String
}

enum InheritanceMode {
    NONE
    MERGE_WITH_PARENT
    OVERRIDE_PARENT
    INHERIT_IF_ABSENT
}