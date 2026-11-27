// A small rpcv2Json service that exercises what SQS and DynamoDB don't: int enums,
// defaults, sparse lists, Unit union members, renamed fields and a retryable error.
$version: "2.1"
namespace example.weather

use smithy.protocols#rpcv2Json

@rpcv2Json
service Weather {
    version: "2026-11-26"
    operations: [GetForecast, Ping]
}

@paginated(inputToken: "token", outputToken: "next", items: "days", pageSize: "size")
operation GetForecast {
    input := {
        @required
        city: String
        size: Integer
        token: String
        units: Units = "METRIC"
        detail: Detail = 1
    }
    output := {
        days: Days
        next: String
        @required
        type: Kind
    }
    errors: [Busy]
}

operation Ping {}

enum Units {
    METRIC
    IMPERIAL
}

intEnum Detail {
    LOW = 1
    HIGH = 2
}

@sparse
list Days { member: Day }

structure Day {
    @timestampFormat("date-time")
    date: Timestamp
    copy: String
}

union Kind {
    sunny: Unit
    rain: Integer
}

@error("server")
@retryable(throttling: true)
structure Busy {
    message: String
}
