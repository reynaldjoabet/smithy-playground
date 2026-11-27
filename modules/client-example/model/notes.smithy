// A small Smithy IDL 2.1 model: member indexes (`1. title`) and inline collections (`[String]`,
// `{String: Integer}`), which the generator turns into plain List and Map types.
$version: "2.1"
namespace example.notes

use smithy.protocols#rpcv2Json

@rpcv2Json
service Notes {
    version: "2026-11-27"
    operations: [AddNote]
}

operation AddNote {
    input := {
        @required
        1. title: String

        2. tags: [String]

        3. scores: {String: Integer}
    }
    output := {
        1. id: String
    }
}
