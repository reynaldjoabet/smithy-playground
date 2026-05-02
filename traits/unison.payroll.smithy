$version: "2.0"

namespace payroll
use unison#tenantScoped

@tenantScoped
@readonly
@http(method: "GET", uri: "/payroll/pay-runs")
operation ListPayRuns {
    input: ListPayRunsInput
    output: ListPayRunsOutput
}

structure ListPayRunsInput {}

structure ListPayRunsOutput {
    payRuns: PayRuns
}

list PayRuns {
    member: PayRun
}

structure PayRun {
    id: String
    status: String
}

// This ensures every operation touching payroll data is explicitly marked as tenant-scoped, and code generators or middleware can enforce row-level filtering automatically.
