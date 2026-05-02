$version: "2.0"
namespace unison.teamDashboard
use unison#tenantScoped
use unison.auth#requiredPermissions
use unison.hierarchy#orgHierarchyScoped

@orgHierarchyScoped(
    hierarchyType: "REPORTING_STRUCTURE"
    visibilityDepth: {
    
        downwardLevels: -1   
        includePeers: false
        upwardLevels: 0       
    }
    delegationRules: {
        allowManagerActsAsReport: true
        allowTransitiveDelegation: true
        maxDelegationDepth: 2
        supportTemporaryDelegation: true
        delegatableScopes: ["time:approve", "leave:approve"]
    }
    inheritanceMode: "INHERIT_IF_ABSENT"
)
@tenantScoped
@readonly
@requiredPermissions(scopes: ["team:read"], minimumRole: "MANAGER")
@http(method: "GET", uri: "/managers/{managerId}/team-dashboard")
operation GetTeamDashboard {
    input: GetTeamDashboardInput
    output: GetTeamDashboardOutput
    errors: [
        ManagerNotFound
        HierarchyTraversalTimeout
        DelegationExpired
    ]
}

structure GetTeamDashboardInput {
    @httpLabel
    @required
    managerId: String
}

structure GetTeamDashboardOutput {}

@error("client")
structure ManagerNotFound {}

@error("client")
structure HierarchyTraversalTimeout {}

@error("client")
structure DelegationExpired {}
