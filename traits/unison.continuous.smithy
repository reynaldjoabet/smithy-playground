$version: "2.0"
namespace unison.continuous

/// Marks an operation as a trigger for Unison's continuous calculation engine.
/// Any mutation annotated with this trait will enqueue affected calculation
/// graphs for re-evaluation upon successful commit.
@trait(selector: "operation")
structure calculationTrigger {
    /// Which calculation domains are invalidated by this mutation
    affectedDomains: CalculationDomainList

    /// The scope of recalculation — how far back and forward to recalculate
    recalculationScope: RecalculationScope

    /// Dependency graph traversal strategy
    propagation: PropagationStrategy

    /// Whether this trigger can be deferred to an async queue
    /// or must complete synchronously within the request
    executionMode: CalcExecutionMode

    /// Maximum acceptable latency for the calculation to converge
    convergenceSlaMs: Long
}

list CalculationDomainList {
    member: CalculationDomain
}

enum CalculationDomain {
    GROSS_PAY
    NET_PAY
    STATUTORY_DEDUCTIONS
    VOLUNTARY_DEDUCTIONS
    BENEFITS_COST
    ACCRUAL_BALANCES
    GL_ENTRIES
    EMPLOYER_CONTRIBUTIONS
    LABOR_ALLOCATION
    WCB_PREMIUMS
    PENSION_CONTRIBUTIONS
    YEAR_TO_DATE_ACCUMULATORS
}

structure RecalculationScope {
    /// How far back to recalculate (for retroactive changes)
    retroactivePeriods: Integer

    /// How far forward to project (for future-dated changes)
    prospectivePeriods: Integer

    /// Whether to cascade through dependent employee records
    /// (e.g., a manager's salary change affecting team labor cost)
    cascadeToDependents: Boolean
}

enum PropagationStrategy {
    FULL_GRAPH_TRAVERSAL
    DIRTY_FLAG_ONLY
    TOPOLOGICAL_SUBGRAPH
}

enum CalcExecutionMode {
    SYNCHRONOUS
    ASYNC_EVENTUAL
    ASYNC_WITH_CALLBACK
    HYBRID_CRITICAL_PATH
}