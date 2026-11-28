// The JSON-RPC 2.0 protocol definition from jsonrpclib (Apache License 2.0), copied from
// https://github.com/neandertech/jsonrpclib/blob/288562fcfe80ec7db07b30aa38feffa4b818f797/modules/smithy/src/main/resources/META-INF/smithy/jsonrpclib.smithy
// Three changes:
//   - the namespace is jsonrpc (jsonrpclib in the original). Models written for jsonrpclib-smithy4s use
//     jsonrpclib#..., so they need their `use` lines changed;
//   - $version is 2.1, like every other .smithy file here;
//   - the four alloy# traits (uuidFormat, discriminated, nullable, untagged) are dropped from the protocol's
//     traits, so the definition needs only Smithy's core traits: nothing here implements them, and without
//     alloy-core on the classpath (as in the VS Code Smithy language server) the definition fails to load.
// Listed in this directory's manifest, so ModelLoader (discoverModels) finds it on the classpath, and models
// can use jsonrpc#jsonRpc without importing a file. jsonrpclib's three validators are ported to Scala in
// smithy.scala.codegen.jsonrpc (one file each) and registered in META-INF/services.
$version: "2.1"

namespace jsonrpc

/// the JSON-RPC protocol,
/// see https://www.jsonrpc.org/specification
@protocolDefinition(traits: [
    jsonRpcRequest
    jsonRpcNotification
    jsonRpcPayload
    smithy.api#jsonName
    smithy.api#length
    smithy.api#pattern
    smithy.api#range
    smithy.api#required
    smithy.api#timestampFormat
])
@trait(selector: "service")
structure jsonRpc {
}

/// Identifies an operation that abides by request/response semantics
/// https://www.jsonrpc.org/specification#request_object
@trait(selector: "operation", conflicts: [jsonRpcNotification])
string jsonRpcRequest

/// Identifies an operation that abides by fire-and-forget semantics
/// see https://www.jsonrpc.org/specification#notification
@trait(selector: "operation", conflicts: [jsonRpcRequest])
string jsonRpcNotification


/// Binds a single structure member to the payload of a jsonrpc message.
/// Just like @httpPayload, but for jsonRpc.
@trait(selector: "structure > member[trait|required]", structurallyExclusive: "member")
@traitValidators({
    "jsonRpcPayload.OnlyTopLevel": {
        message: "jsonRpcPayload can only be used on the top level of an operation input/output/error.",
        severity: "ERROR",
        selector: "$allowedShapes(:root(operation -[input, output, error]-> structure > member)) :not(:in(${allowedShapes}))"
    }
})
structure jsonRpcPayload {}
