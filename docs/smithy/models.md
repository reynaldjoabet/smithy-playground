# The model

Everything starts from a Smithy model. A generator never sees a service any other way: if the model doesn't say it, the SDK can't do it, unless someone writes a customization by hand.

## Shapes and traits

A model is a set of **shapes** with **traits** attached:

- **Shapes** are the types: `service`, `operation`, `resource`, `structure`, `union`, `list`, `map`, `enum`, `intEnum`, and simple types such as `string`, `integer`, `timestamp`, `blob` and `document`. Each has an absolute id such as `com.amazonaws.sqs#SendMessage`.
- **Traits** are typed annotations on shapes and members. Some constrain values (`@required`, `@length`, `@pattern`), some describe wire behaviour (`@http`, `@jsonName`, `@xmlFlattened`), and some describe the service (`@aws.auth#sigv4`, `@aws.protocols#awsJson1_0`).

The **service closure** is everything reachable from the `service` shape: its operations, their input and output structures, errors, and every shape those refer to. Generators walk this closure, so shapes outside it aren't generated.

For SQS, the closure looks like this:

```text
AmazonSQS (service)
│   @awsJson1_0  @awsQueryCompatible  @sigv4(name: "sqs")  @endpointRuleSet  @endpointBdd
│
├── SendMessage (operation)
│   ├── input:  SendMessageRequest (structure)
│   │   ├── QueueUrl       -> String           @required
│   │   ├── MessageBody    -> String           @required
│   │   ├── DelaySeconds   -> NullableInteger
│   │   └── MessageAttributes -> MessageBodyAttributeMap -> MessageAttributeValue (structure)
│   ├── output: SendMessageResult (structure)
│   │   └── MessageId, MD5OfMessageBody, SequenceNumber, ...
│   └── errors: QueueDoesNotExist   @error("client")  @httpError(400)  @awsQueryError(...)
│               InvalidMessageContents, KmsAccessDenied, RequestThrottled, ...
│
├── ListQueues (operation)   @paginated(inputToken: NextToken, items: QueueUrls)
│   └── ...
│
└── 21 more operations ...   every shape reachable from here is the service closure
```

The same model can be written in the Smithy IDL (`.smithy`) or as a JSON AST. AWS publishes its service models as JSON AST, one file per service, in [aws/api-models-aws](https://github.com/aws/api-models-aws). This playground copies them into [src/main/resources/models](../../src/main/resources/models) with [scripts/update-aws-models.sh](../../scripts/update-aws-models.sh).

## A real example: SQS

This is the service shape of SQS from [aws-sdk-go-v2/codegen/sdk-codegen/aws-models/sqs.json](https://github.com/aws/aws-sdk-go-v2/blob/main/codegen/sdk-codegen/aws-models/sqs.json), with the documentation and endpoint rules left out:

```json
"com.amazonaws.sqs#AmazonSQS": {
  "type": "service",
  "version": "2012-11-05",
  "operations": [ ... ],
  "traits": {
    "aws.api#service": {
      "sdkId": "SQS",
      "arnNamespace": "sqs",
      "cloudFormationName": "SQS",
      "cloudTrailEventSource": "sqs.amazonaws.com",
      "endpointPrefix": "sqs"
    },
    "aws.auth#sigv4": { "name": "sqs" },
    "aws.protocols#awsJson1_0": {},
    "aws.protocols#awsQueryCompatible": {},
    "smithy.rules#endpointRuleSet": { ... },
    "smithy.rules#endpointBdd": { ... },
    "smithy.rules#endpointTests": { ... }
  }
}
```

Each trait drives part of the generated SDK:

| Trait | What the SDK does with it |
|---|---|
| `aws.api#service` `sdkId: "SQS"` | Names the package or module: `service/sqs` in Go, `software.amazon.awssdk.services.sqs` in Java. |
| `aws.auth#sigv4` `name: "sqs"` | Registers the SigV4 auth scheme with signing name `sqs`, which goes into the credential scope (`…/us-east-1/sqs/aws4_request`). |
| `aws.protocols#awsJson1_0` | Picks the wire protocol: `POST /`, an `X-Amz-Target: AmazonSQS.SendMessage` header and a JSON body. |
| `aws.protocols#awsQueryCompatible` | SQS moved from the Query protocol to JSON. With this trait, the client also reads the old Query error code from the `x-amzn-query-error` header, so existing error handling keeps working. |
| `smithy.rules#endpointRuleSet` | A small program, written in JSON, that computes the endpoint URL from parameters such as `Region`, `UseFIPS` and `UseDualStack`. |
| `smithy.rules#endpointBdd` | The same rules compiled into a binary decision diagram. Newer generators evaluate this instead of the rule tree (Go's `endpoints.go` calls `bdd.Evaluate`). |
| `smithy.rules#endpointTests` | Test cases for the rules; generators turn them into `endpoints_test.go` and similar tests. |

An operation and its input look like this:

```json
"com.amazonaws.sqs#SendMessage": {
  "type": "operation",
  "input":  { "target": "com.amazonaws.sqs#SendMessageRequest" },
  "output": { "target": "com.amazonaws.sqs#SendMessageResult" },
  "errors": [ { "target": "com.amazonaws.sqs#QueueDoesNotExist" }, ... ]
},
"com.amazonaws.sqs#SendMessageRequest": {
  "type": "structure",
  "members": {
    "QueueUrl":     { "target": "com.amazonaws.sqs#String", "traits": { "smithy.api#required": {} } },
    "MessageBody":  { "target": "com.amazonaws.sqs#String", "traits": { "smithy.api#required": {} } },
    "DelaySeconds": { "target": "com.amazonaws.sqs#NullableInteger" },
    ...
  }
}
```

And an error:

```json
"com.amazonaws.sqs#QueueDoesNotExist": {
  "type": "structure",
  "traits": {
    "smithy.api#error": "client",
    "smithy.api#httpError": 400,
    "aws.protocols#awsQueryError": { "code": "AWS.SimpleQueueService.NonExistentQueue", "httpResponseCode": 400 }
  }
}
```

`@error("client")` makes it an exception type in Java and an error struct in Go. The protocol's error deserializer reads the error code from the response (for awsJson, the `__type` field or the `X-Amzn-Errortype` header) and maps it to this shape.

## Traits that SDKs depend on

Beyond protocol and auth, these traits are what make a generated client more than a typed HTTP wrapper:

| Trait | Generated behaviour |
|---|---|
| `@required`, `@length`, `@range`, `@pattern` | `@required` becomes client-side validation (Go's `validators.go`). Other constraints are usually left to the server, which owns them. |
| `@default` | Default values in builders and structs, and whether a member is nullable (`*int32` or `int32` in Go). |
| `@idempotencyToken` | The client fills the member with a UUID if you leave it empty, so a retried create doesn't create twice. |
| `@paginated` | A paginator type. On `ListQueues` it is `{inputToken: NextToken, outputToken: NextToken, items: QueueUrls, pageSize: MaxResults}`. |
| `@waitable` | Waiters such as `WaitUntilTableExists`, which poll an operation until a JMESPath condition on the output holds. |
| `@retryable`, `@aws.api#clientEndpointDiscovery` and similar | Extra retry and endpoint behaviour for specific errors and services. |
| `@httpChecksum`, `@httpChecksumRequired` | Request checksums and response checksum validation (for example the `x-amz-checksum-*` headers on S3). |
| `@streaming`, `@eventStream` | Streaming bodies (S3 objects) and event streams (Transcribe, Bedrock), which need different client APIs. |
| `@deprecated`, `@documentation`, `@examples` | Deprecation annotations, doc comments and code examples in the generated sources. |

## Where each repository gets its models

| Repository | Model input | Notes |
|---|---|---|
| aws-sdk-go-v2 | [codegen/sdk-codegen/aws-models/*.json](https://github.com/aws/aws-sdk-go-v2/tree/main/codegen/sdk-codegen/aws-models): Smithy JSON AST, one file per service, 446 files | The same format as aws/api-models-aws. Each file must contain exactly one service, which the build checks. |
| smithy-go | Test models in [codegen/smithy-go-codegen-test/model](https://github.com/aws/smithy-go/tree/main/codegen/smithy-go-codegen-test/model) | smithy-go is the generator, not an SDK; it ships no service models. |
| smithy-java | Your own `.smithy` or JSON files, named in `smithy-build.json`; examples in [examples/](https://github.com/smithy-lang/smithy-java/tree/main/examples) | Works with any Smithy model, AWS or not. The `aws-service-bundle` modules package AWS models for runtime use. |
| aws-sdk-java-v2 | [services/&lt;name&gt;/src/main/resources/codegen-resources/](https://github.com/aws/aws-sdk-java-v2/tree/master/services/sqs/src/main/resources/codegen-resources): `service-2.json`, `paginators-1.json`, `waiters-2.json`, `endpoint-rule-set.json` or `endpoint-bdd-1.json`, `customization.config` | A different JSON format ("C2J") that splits one service across several files. It carries the same information: SQS's `metadata` has `"protocol": "json"`, `"targetPrefix": "AmazonSQS"`, `"awsQueryCompatible": {}` and `"auth": ["aws.auth#sigv4"]`. |

The C2J format is why aws-sdk-java-v2 doesn't use a `smithy-build` plugin: its generator was written before Smithy, around that format, and still reads it. Its runtime has since adopted the parts of the Smithy reference architecture that matter at call time (identity and auth schemes, endpoint rules, the `rpcv2Cbor` protocol), as [the request lifecycle](request-lifecycle.md) shows.

## Generator repositories and SDK repositories

A Smithy generator repository holds the generator and the runtime, and tests against its own small models. The full set of AWS models usually lives with the SDK built from it. What differs is which repository runs the build. The counts below are files or directories in each repository's default branch on 2026-11-26. "Not counted" means the repository wasn't checked.

| Generator | AWS models in the generator repository | AWS SDK, and where its models live | Model format | Who runs the generator |
|---|---|---|---|---|
| smithy-go | 0 | [aws-sdk-go-v2](https://github.com/aws/aws-sdk-go-v2): `codegen/sdk-codegen/aws-models/` (446) | Smithy JSON AST | aws-sdk-go-v2's Gradle build feeds each model to `go-codegen` |
| smithy-kotlin | 0 | [aws-sdk-kotlin](https://github.com/awslabs/aws-sdk-kotlin): `codegen/sdk/aws-models/` (446) | Smithy JSON AST | aws-sdk-kotlin's build, with its own `codegen/aws-sdk-codegen` (smithy-kotlin's `codegen/aws-codegen` has the AWS protocol generators) |
| smithy-rs | 27, a subset in `aws/sdk/aws-models/` | [aws-sdk-rust](https://github.com/awslabs/aws-sdk-rust): `aws-models/` (450+) | Smithy JSON AST | smithy-rs itself: its `aws/sdk` project generates the SDK, reading aws-sdk-rust's models through `-Paws.sdk.models.path` |
| smithy-swift | not counted | [aws-sdk-swift](https://github.com/awslabs/aws-sdk-swift): `codegen/sdk-codegen/aws-models/` (446) | Smithy JSON AST | aws-sdk-swift's `codegen/` build |
| smithy-typescript | not counted | [aws-sdk-js-v3](https://github.com/aws/aws-sdk-js-v3): `codegen/sdk-codegen/aws-models/` (about 446) | Smithy JSON AST | aws-sdk-js-v3's `codegen/` build |
| smithy-python | not counted | [aws-sdk-python](https://github.com/awslabs/aws-sdk-python), the AWS SDK for Python v4 in Developer Preview: `codegen/aws-models/` (39), generated into `clients/` (40 packages) | Smithy JSON AST | aws-sdk-python's `codegen/` Gradle build (it has a `smithy-python.toml`) |
| smithy-java | not counted; its `aws-service-bundle` modules package AWS models for runtime use | none: the AWS SDK for Java is aws-sdk-java-v2, below | | you, through `smithy-build` |
| none (aws-sdk-java-v2's own generator) | | [aws-sdk-java-v2](https://github.com/aws/aws-sdk-java-v2): `services/<name>/src/main/resources/codegen-resources/` (446 services) | C2J `service-2.json` | aws-sdk-java-v2's `codegen-maven-plugin` |
| none (aws-sdk-ruby's own generator) | | [aws-sdk-ruby](https://github.com/aws/aws-sdk-ruby): `apis/<service>/<version>/` (448) | C2J `api-2.json`, with paginators, waiters and endpoint rules | aws-sdk-ruby's `build_tools/aws-sdk-code-generator`, not smithy-ruby |
| none (no generator) | | [aws-sdk-php](https://github.com/aws/aws-sdk-php): `src/data/<service>/<version>/` (461) | C2J `api-2.json`, plus `.json.php` copies | nothing: the PHP SDK loads the model data at runtime, and smithy-php isn't involved |

So:
- **The SDK repository holds the models and runs the generator** for Go, Kotlin, Swift, TypeScript and Python.
- **Rust is reversed.** The generator repository runs the build, and aws-sdk-rust holds the models and the published output.
- **Java, Ruby and PHP** use C2J models with their own tools, so a Smithy generator doesn't take part. C2J is a rendering of the same service, which is why each count is close to the 446 Smithy models.


Next: [Code generation](code-generation.md).
