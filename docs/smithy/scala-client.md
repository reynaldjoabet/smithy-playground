# A Scala client generator

This playground includes a small Smithy client generator for Scala 3, built the way the earlier pages describe: generated code holds only shapes and schemas, and a hand-written runtime holds the codec, the protocols, auth, the transport and the retry pipeline. It's a teaching implementation, not a production SDK; [what it leaves out](#what-it-leaves-out) is listed at the end.

## The modules

| sbt project | Directory | What it is |
|---|---|---|
| `clientRuntime` | [modules/client-runtime](../../modules/client-runtime) | The runtime generated code runs on. Depends on Jackson 3 (`tools.jackson.core`) for Smithy JSON, plus ZIO Blocks and Apache Avro for the [extra formats](#other-formats-through-zio-blocks). |
| `clientCodegen` | [modules/client-codegen](../../modules/client-codegen) | The generator, in package `smithy.scala.codegen`. Reads a model with the Smithy Java libraries and writes Scala sources. Also a smithy-build plugin, `scala-client-codegen` (`ScalaClientCodegenPlugin`). `scala-server-codegen` (`ScalaServerCodegenPlugin`) is registered too, but only as a stub that fails with "not implemented yet". |
| `clientBench` | [modules/bench](../../modules/bench) | JMH benchmarks (sbt-jmh): Jackson's `JsonCodec` against the ZIO Blocks codecs ([results](#speed-jackson-against-zio-blocks)). |
| `clientExample` | [modules/client-example](../../modules/client-example) | Generated SQS and DynamoDB (`awsJson1_0`), KMS, Secrets Manager and Cognito Identity Provider (`awsJson1_1`), Weather (`rpcv2Json`), and Notes (`rpcv2Json`, written in Smithy IDL 2.1) clients, with codecs derived by Hearth (the default), and tests that run them against a local HTTP server. `EmulatorDemo` runs SQS, DynamoDB, Secrets Manager and Cognito against the AWS emulator (`runMain example.EmulatorDemo secretsmanager cognito`). Its tests also hold Mirror-derived copies of the DynamoDB and Weather clients (`example.mirror.*`). |
| `clientHearth` | [modules/hearth](../../modules/hearth) | The [Hearth derivation](#the-hearth-derivation-the-default) that generated code uses by default. |

Like aws-sdk-go-v2, the generated code is written into the source tree rather than generated during the build, so it can be read and reviewed. Regenerate it with:

```sh
scripts/generate-scala-clients.sh
```

The script runs `sbt --client "clientCodegen/runMain smithy.scala.codegen.Main <model> <service id> <package> <output dir>"` once per service. Each run replaces the package directory it writes.

## Each layer, and where it lives

| Layer | File | What it does here |
|---|---|---|
| Schemas | [Schema.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Schema.scala) | `Schema` carries a shape's id, type, members and runtime traits. A member is a schema too; its target is a thunk, so recursive shapes such as DynamoDB's `AttributeValue` work. |
| Serde interfaces | [Serde.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Serde.scala) | `ShapeWriter` and `ShapeReader`: format-neutral calls such as `writeString(memberSchema, value)`. Nothing above the codec sees a format. |
| Shape codecs | [ShapeCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/ShapeCodec.scala), [Defaults.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Defaults.scala) | `ShapeCodec[A]` walks one Scala type with those calls. Primitives, `Option`, `List` and `Map` are givens; structures, unions and enum values are derived at compile time (see [below](#scala-3-features-in-use)). |
| Codec | [codecs/](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs) | [JsonCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/JsonCodec.scala) is the implemented one: `JsonSettings` says whether to honour `@jsonName` and `@timestampFormat`, because protocols differ on that. `XmlCodec`, `FormUrlCodec`, `CborCodec` and `ProtobufCodec` are placeholders behind the same `Codec` trait. Each documents the traits and rules it would follow, and fails with `CodecNotImplemented` if used. ZIO Blocks' own JSON, MessagePack, Avro, YAML and Thrift go through ZIO Blocks ([below](#other-formats-through-zio-blocks)). |
| Protocols | [Protocols.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Protocols.scala) | `AwsJsonProtocol` (`X-Amz-Target`, `application/x-amz-json-1.0`) and `RpcV2JsonProtocol` (`/service/<S>/operation/<O>`, `Smithy-Protocol: rpc-v2-json`). Both share the error handling: find the code, look it up in the `TypeRegistry`, let the codec fill the error. |
| Errors | [Operation.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Operation.scala) | `ServiceException` (generated errors extend it), `UnknownServiceException`, `TypeRegistry.of[E1 *: E2 *: EmptyTuple]`, and `Operation.of[I, O]`, which bundles one operation's codecs, errors and auth schemes. |
| Auth | [Auth.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Auth.scala) | `CredentialsProvider` (the identity resolver) and `SigV4`, checked against cases from the AWS SigV4 test suite. |
| Transport | [Http.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Http.scala) | `HttpTransport`, with `JavaHttpTransport` on `java.net.http`. It can take an `SSLContext`, for example one that trusts the AWS Emulator's certificate. |
| Pipeline | [Pipeline.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Pipeline.scala) | `ClientPipeline.invoke`: resolve the endpoint and serialize once, then per attempt run interceptors, sign, send and deserialize, inside the retry loop. Also `RetryStrategy`, `Interceptor` and `Paginator`. |

The protocol is chosen from the service's protocol trait when the client is generated, and the same generated shapes would work with any other JSON protocol. Adding `restJson1` would mean a new class in `Protocols.scala` that reads the HTTP binding traits; the generator would only need to add those traits to the runtime traits it copies into schemas.

## Schema, shape codec and format codec

The schema doesn't convert anything. In this design the schema only describes; the codec does the conversion, using the schema as its guide. Three parts work together:

| Part | Role | In this repo |
|---|---|---|
| **Schema** | Data about a shape: its id, type, members (name and position) and the traits that matter at runtime. It contains no conversion logic. | `Schema`, `MemberDef`, `Traits` in [Schema.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Schema.scala) |
| **Shape codec** (derived or generated) | Walks a Scala value and says *what* to write: "member `QueueName` is the string `orders`". It calls a format-neutral writer and knows no format. | `ShapeCodec[A]` / `SchemaCodec[A]`, from the `Mirror` derivation or Hearth's `HearthCodec.derived` |
| **Format codec** | Decides *how* that becomes bytes: quotes, field names, number and timestamp encoding, base64 for blobs. | `JsonCodec` (`JsonShapeWriter`, `JsonShapeReader`) in [JsonCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/JsonCodec.scala) |

One write, step by step:

```scala
// the shape codec, for SendMessageRequest:
w.writeStruct(schema) { s =>
  s.writeString(schema.members(0), value.queueUrl)   // "member 0 is this string"
  ...
}
// the JSON codec, inside writeString:
gen.writeName(fieldName(member))                      // member name, or @jsonName if the protocol allows it
gen.writeString(value)                                // JSON string syntax
```

The schema supplies the facts the format codec reads:
- the member's name (`QueueUrl`);
- its `@jsonName`;
- its `@timestampFormat`.

The protocol decides which of those apply, through `JsonSettings`. `restJson1` honours `@jsonName`; `awsJson1_0` and `rpcv2Json` ignore it.

The payoff is that one schema serves several formats. An XML codec reading the same `Schema` would look for `@xmlName` and `@xmlFlattened` and write `<QueueUrl>…</QueueUrl>`. A Protobuf codec would read `@idx` and write field numbers. Neither the schema nor the generated types change.

So the schema is the map, the shape codec walks the value along it, and the format codec writes the bytes. [The protocol stack](protocol-stack.md#what-each-part-does) has the full table of who does what, including the protocol (where things go in HTTP) and the transport.

### One value, end to end

Our runtime `Schema` is the source description, Hearth writes the code that connects it to the Scala classes, and an Avro schema or JSON Schema is a format-specific description derived from it. Here's one value going through all of them:

```scala
GetForecastOutput(next = Some("t"), `type` = Kind.Rain(3))
```

**1. Our `Schema`: the shape as plain data.** The generator copies the part of the Smithy model the runtime needs into each type's companion:

```scala
object GetForecastOutput {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("example.weather", "GetForecastOutput"), rt.ShapeType.Structure, ...)(
    rt.MemberDef("days", () => Days.schema),
    rt.MemberDef("next", () => rt.Prelude.String),
    rt.MemberDef("type", () => Kind.schema, rt.Traits("smithy.api#required" -> ""))
  )
  given codec: rt.SchemaCodec[GetForecastOutput] = smithy.scala.hearth.HearthCodec.derived[GetForecastOutput](schema)
}
// Kind.schema: a Union with members sunny (0, Unit) and rain (1, Integer)
```

It says the shape has members `days`, `next` and `type`, in that order, with their types and a few traits. It contains no code: it can't read `value.next` or build a `GetForecastOutput`. It's the Smithy model, carried into the runtime.

**2. Hearth: the code that connects the schema to the Scala class.** `HearthCodec.derived` is a macro. At compile time it looks at the case class, pairs each field with its member schema, and writes code that does roughly this:

```scala
w.writeStruct(schema) { s =>
  value.days.foreach(d => writeDays(s, schema.members(0), d))   // None: nothing written
  value.next.foreach(n => s.writeString(schema.members(1), n))  // "member next is the string t"
  value.`type` match {                                          // Scala case → Smithy union member
    case Kind.Rain(x) => s.writeStruct(schema.members(2)) { u => u.writeInteger(Kind.schema.members(1), x) }
    ...
  }
}
```

So Hearth knows the Scala side: field getters, the constructor, `Option`, defaults, which enum case is which union member. It knows no format. Its output is a series of calls like "write this member with this value", each carrying the member's schema. Reading is the same in reverse: the reader says "here's member `next`", and the generated code puts it in the constructor.

**3. The format codec: uses the schema to decide the bytes.** The format codec receives those calls and reads the member schema to decide how each value looks.

JSON (`JsonCodec`) uses the member's *name* (or `@jsonName`):

```json
{"next":"t","type":{"rain":3}}
```

Avro (`AvroSchemaCodec`) ignores names and uses each member's *position* and the union's branch index:

```text
00        days: null branch (absent)
02        next: present
02 74     "t" (length 1, then the byte 't')
02        type: present
02        union branch 1 = rain
06        3
```

Seven bytes, because the reader already knows the layout: days, then next, then type.

**4. Avro schema and JSON Schema: descriptions for other tools.** The Avro reader can only decode those 7 bytes if it has the layout. That's what an Avro schema is, and `AvroSchemas.of` builds one from our `Schema` (shortened):

```json
{ "type": "record", "name": "GetForecastOutput", "namespace": "example.weather",
  "fields": [
    { "name": "days", "type": ["null", {"type": "array", ...}], "default": null },
    { "name": "next", "type": ["null", "string"], "default": null },
    { "name": "type", "type": ["null", { "type": "record", "name": "Kind",
        "fields": [{ "name": "member", "type": [ {"name": "Kind_sunny", ...}, {"name": "Kind_rain", "fields": [{"name": "value", "type": "int"}]} ] }] }],
      "default": null } ] }
```

Our codec doesn't need this file to write or read, because it reads our `Schema` directly. It exists for other Avro tools, such as a schema registry, Kafka consumers or Spark, so they can read the same bytes.

A JSON Schema would play the same role for the JSON output: a description others use to validate or document `{"next":"t","type":{"rain":3}}`. This repo doesn't generate one. It could be derived the same way:
- from the runtime `Schema`, like `AvroSchemas.of`;
- or straight from the Smithy model with Smithy's own JSON Schema and OpenAPI converters.

Who knows what:

| Piece | Knows | Doesn't know | When |
|---|---|---|---|
| Smithy model | Everything: shapes, traits, operations | Scala, bytes | Build time |
| Our `Schema` | Member names, order, types, a few traits | How to get or build Scala values; any format | Runtime data |
| Hearth codec | The Scala class: getters, constructor, `Option`, enum cases | Any format | Generated at compile time, runs at runtime |
| Format codec (`JsonCodec`, `AvroSchemaCodec`) | One format's rules | The Scala class | Runtime |
| Avro schema, JSON Schema | One format's layout, for other tools | Scala | Derived from our `Schema` or from the model |

### Three AWS examples

The same four parts, on real SQS and DynamoDB shapes. The JSON is what the awsJson1_0 codec writes, as SQS and DynamoDB expect, and the Avro bytes come from `AvroSchemaCodec`.

Keep one picture in mind for each:
- the **model** says what exists;
- the **schema** is the runtime copy of that;
- **Hearth** links the schema to the Scala class;
- the **format codec** turns it into bytes.

#### SQS `SendMessage`: names, required vs optional, nested structures

**Model** ([sqs.json](../../src/main/resources/models/sqs/sqs.json), documentation removed):

```json
"com.amazonaws.sqs#SendMessage": {
  "type": "operation",
  "input":  { "target": "com.amazonaws.sqs#SendMessageRequest" },
  "output": { "target": "com.amazonaws.sqs#SendMessageResult" },
  "errors": [ InvalidAddress, InvalidMessageContents, InvalidSecurity, KmsAccessDenied, KmsDisabled,
              KmsInvalidKeyUsage, KmsInvalidState, KmsNotFound, KmsOptInRequired, KmsThrottled,
              QueueDoesNotExist, RequestThrottled, UnsupportedOperation ]
},
"com.amazonaws.sqs#SendMessageRequest": {
  "type": "structure",
  "members": {
    "QueueUrl":                { "target": "com.amazonaws.sqs#String", "traits": { "smithy.api#required": {} } },
    "MessageBody":             { "target": "com.amazonaws.sqs#String", "traits": { "smithy.api#required": {} } },
    "DelaySeconds":            { "target": "com.amazonaws.sqs#NullableInteger" },
    "MessageAttributes":       { "target": "com.amazonaws.sqs#MessageBodyAttributeMap",
                                 "traits": { "smithy.api#xmlFlattened": {}, "smithy.api#xmlName": "MessageAttribute" } },
    "MessageSystemAttributes": { "target": "com.amazonaws.sqs#MessageBodySystemAttributeMap",
                                 "traits": { "smithy.api#xmlFlattened": {}, "smithy.api#xmlName": "MessageSystemAttribute" } },
    "MessageDeduplicationId":  { "target": "com.amazonaws.sqs#String" },
    "MessageGroupId":          { "target": "com.amazonaws.sqs#String" }
  },
  "traits": { "smithy.api#input": {} }
},
"com.amazonaws.sqs#MessageBodyAttributeMap": {
  "type": "map",
  "key":   { "target": "com.amazonaws.sqs#String",                "traits": { "smithy.api#xmlName": "Name" } },
  "value": { "target": "com.amazonaws.sqs#MessageAttributeValue", "traits": { "smithy.api#xmlName": "Value" } }
},
"com.amazonaws.sqs#MessageAttributeValue": {
  "type": "structure",
  "members": {
    "StringValue":      { "target": "com.amazonaws.sqs#String" },
    "BinaryValue":      { "target": "com.amazonaws.sqs#Binary" },
    "StringListValues": { "target": "com.amazonaws.sqs#StringList", "traits": { "smithy.api#xmlFlattened": {}, "smithy.api#xmlName": "StringListValue" } },
    "BinaryListValues": { "target": "com.amazonaws.sqs#BinaryList", "traits": { "smithy.api#xmlFlattened": {}, "smithy.api#xmlName": "BinaryListValue" } },
    "DataType":         { "target": "com.amazonaws.sqs#String", "traits": { "smithy.api#required": {} } }
  }
}
```

`members` is an object keyed by member name, not an array, because member names are unique within a shape. Smithy's loader keeps the keys in file order, and the generator lays out fields and `MemberDef`s in that order. JSON doesn't care about that order, but `AvroSchemaCodec`, which writes by position, does.

**What the generator writes**: one file per shape. The `rt` prefix is `import smithy.scala.{runtime as rt}`.

[SendMessageRequest.scala](../../modules/client-example/src/main/scala/example/sqs/SendMessageRequest.scala): a structure, so it gets a case class, a schema and a Hearth codec.

```scala
// Generated by smithy.scala.codegen from com.amazonaws.sqs#AmazonSQS. Do not edit.
package example.sqs

import smithy.scala.{runtime as rt}

final case class SendMessageRequest(
    queueUrl: java.lang.String,
    messageBody: java.lang.String,
    delaySeconds: scala.Option[scala.Int] = scala.None,
    messageAttributes: scala.Option[MessageBodyAttributeMap] = scala.None,
    messageSystemAttributes: scala.Option[MessageBodySystemAttributeMap] = scala.None,
    messageDeduplicationId: scala.Option[java.lang.String] = scala.None,
    messageGroupId: scala.Option[java.lang.String] = scala.None
)

object SendMessageRequest {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("com.amazonaws.sqs", "SendMessageRequest"), rt.ShapeType.Structure, rt.Traits.empty)(
    rt.MemberDef("QueueUrl", () => rt.Prelude.String, rt.Traits("smithy.api#required" -> "")),
    rt.MemberDef("MessageBody", () => rt.Prelude.String, rt.Traits("smithy.api#required" -> "")),
    rt.MemberDef("DelaySeconds", () => rt.Prelude.Integer, rt.Traits.empty),
    rt.MemberDef("MessageAttributes", () => MessageBodyAttributeMap.schema, rt.Traits.empty),
    rt.MemberDef("MessageSystemAttributes", () => MessageBodySystemAttributeMap.schema, rt.Traits.empty),
    rt.MemberDef("MessageDeduplicationId", () => rt.Prelude.String, rt.Traits.empty),
    rt.MemberDef("MessageGroupId", () => rt.Prelude.String, rt.Traits.empty)
  )

  given codec: rt.SchemaCodec[SendMessageRequest] = smithy.scala.hearth.HearthCodec.derived[SendMessageRequest](schema)
}
```

[MessageBodyAttributeMap.scala](../../modules/client-example/src/main/scala/example/sqs/MessageBodyAttributeMap.scala): a map, so it gets a type alias and a schema with `key` and `value` members. It has no codec of its own: the runtime's generic `Map` codec reads the value schema from the member.

```scala
type MessageBodyAttributeMap = scala.collection.immutable.Map[java.lang.String, MessageAttributeValue]

object MessageBodyAttributeMap {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("com.amazonaws.sqs", "MessageBodyAttributeMap"), rt.ShapeType.Map, rt.Traits.empty)(
    rt.MemberDef("key", () => rt.Prelude.String, rt.Traits.empty),
    rt.MemberDef("value", () => MessageAttributeValue.schema, rt.Traits.empty)
  )
}
```

[MessageAttributeValue.scala](../../modules/client-example/src/main/scala/example/sqs/MessageAttributeValue.scala): a structure again. Its one required member, `dataType`, comes last in the model, so it's the last constructor parameter.

```scala
/** The user-specified message attribute value. */
final case class MessageAttributeValue(
    stringValue: scala.Option[java.lang.String] = scala.None,
    binaryValue: scala.Option[rt.Blob] = scala.None,
    stringListValues: scala.Option[StringList] = scala.None,
    binaryListValues: scala.Option[BinaryList] = scala.None,
    dataType: java.lang.String
)

object MessageAttributeValue {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("com.amazonaws.sqs", "MessageAttributeValue"), rt.ShapeType.Structure, rt.Traits.empty)(
    rt.MemberDef("StringValue", () => rt.Prelude.String, rt.Traits.empty),
    rt.MemberDef("BinaryValue", () => rt.Prelude.Blob, rt.Traits.empty),
    rt.MemberDef("StringListValues", () => StringList.schema, rt.Traits.empty),
    rt.MemberDef("BinaryListValues", () => BinaryList.schema, rt.Traits.empty),
    rt.MemberDef("DataType", () => rt.Prelude.String, rt.Traits("smithy.api#required" -> ""))
  )

  given codec: rt.SchemaCodec[MessageAttributeValue] = smithy.scala.hearth.HearthCodec.derived[MessageAttributeValue](schema)
}
```

[StringList.scala](../../modules/client-example/src/main/scala/example/sqs/StringList.scala) and [BinaryList.scala](../../modules/client-example/src/main/scala/example/sqs/BinaryList.scala): lists, so each gets a type alias and a schema with one `member`.

```scala
type StringList = scala.collection.immutable.List[java.lang.String]
object StringList {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("com.amazonaws.sqs", "StringList"), rt.ShapeType.List, rt.Traits.empty)(
    rt.MemberDef("member", () => rt.Prelude.String, rt.Traits.empty)
  )
}

type BinaryList = scala.collection.immutable.List[rt.Blob]
object BinaryList {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("com.amazonaws.sqs", "BinaryList"), rt.ShapeType.List, rt.Traits.empty)(
    rt.MemberDef("member", () => rt.Prelude.Blob, rt.Traits.empty)
  )
}
```

[MessageBodySystemAttributeMap.scala](../../modules/client-example/src/main/scala/example/sqs/MessageBodySystemAttributeMap.scala): a map whose key is an **enum**.

```scala
type MessageBodySystemAttributeMap = scala.collection.immutable.Map[java.lang.String, MessageSystemAttributeValue]

object MessageBodySystemAttributeMap {
  lazy val schema: rt.Schema = rt.Schema.aggregate(rt.ShapeId("com.amazonaws.sqs", "MessageBodySystemAttributeMap"), rt.ShapeType.Map, rt.Traits.empty)(
    rt.MemberDef("key", () => MessageSystemAttributeNameForSends.schema, rt.Traits.empty),
    rt.MemberDef("value", () => MessageSystemAttributeValue.schema, rt.Traits.empty)
  )
}
```

[MessageSystemAttributeNameForSends.scala](../../modules/client-example/src/main/scala/example/sqs/MessageSystemAttributeNameForSends.scala): an enum, which is open. It gets a simple schema, a codec that maps strings in and out, and `UnknownValue` for values added later.

```scala
enum MessageSystemAttributeNameForSends(val value: java.lang.String) derives CanEqual {
  case AWSTraceHeader extends MessageSystemAttributeNameForSends("AWSTraceHeader")

  /** A value this client doesn't know (added to the model later). */
  case UnknownValue(raw: java.lang.String) extends MessageSystemAttributeNameForSends(raw)
}

object MessageSystemAttributeNameForSends {
  lazy val schema: rt.Schema = rt.Schema.simple(rt.ShapeId("com.amazonaws.sqs", "MessageSystemAttributeNameForSends"), rt.ShapeType.Enum)

  /** The known values, found from the enum's Mirror at compile time. */
  lazy val knownValues: scala.collection.immutable.List[MessageSystemAttributeNameForSends] = smithy.scala.hearth.HearthCodec.enumValues[MessageSystemAttributeNameForSends]

  def fromValue(value: java.lang.String): MessageSystemAttributeNameForSends = knownValues.find(_.value == value).getOrElse(UnknownValue(value))

  given codec: rt.ShapeCodec[MessageSystemAttributeNameForSends] = rt.ShapeCodec.stringEnum(fromValue, _.value)
}
```

[MessageSystemAttributeValue.scala](../../modules/client-example/src/main/scala/example/sqs/MessageSystemAttributeValue.scala) has the same five members as `MessageAttributeValue`, under its own name and shape id.

**How the client uses them** ([SQSClient.scala](../../modules/client-example/src/main/scala/example/sqs/SQSClient.scala)):

```scala
trait SQSClient {
  def sendMessage(input: SendMessageRequest): SendMessageResult
  ...
}

object Operations {
  lazy val SendMessage: rt.Operation[SendMessageRequest, SendMessageResult] =
    rt.Operation.of[SendMessageRequest, SendMessageResult](
      rt.ShapeId("com.amazonaws.sqs", "SendMessage"),
      rt.TypeRegistry.of[InvalidAddress *: InvalidMessageContents *: InvalidSecurity *: KmsAccessDenied *: KmsDisabled *:
                         KmsInvalidKeyUsage *: KmsInvalidState *: KmsNotFound *: KmsOptInRequired *: KmsThrottled *:
                         QueueDoesNotExist *: RequestThrottled *: UnsupportedOperation *: EmptyTuple],
      List("aws.auth#sigv4"))
}

def sendMessage(input: SendMessageRequest): SendMessageResult = pipeline.invoke(Operations.SendMessage, input)
```

`Operation.of` finds the `given codec` for `SendMessageRequest` and `SendMessageResult`. When you call `sendMessage`:
1. The awsJson protocol calls `SendMessageRequest.codec.write`, which is the Hearth code.
2. That code walks the case class and calls `JsonCodec` once per member, passing its schema.
3. `JsonCodec` writes `"QueueUrl":…`, and so on, recursing into the map and `MessageAttributeValue`.
4. The request is signed with SigV4 and sent.
5. The response comes back the same way in reverse. An error is matched against the `TypeRegistry`.

**Where each model detail ends up:**

| In the model | In the generated code |
|---|---|
| Member names (`QueueUrl`) | The schema (`MemberDef("QueueUrl", …)`); the Scala field is `queueUrl` |
| `@required` | A plain field (not `Option`), and a `"smithy.api#required"` trait in the schema |
| No `@required` | `Option[…] = None` |
| `@xmlFlattened`, `@xmlName` | Dropped. Only the XML protocols read them (see [Which traits the generator keeps](#which-traits-the-generator-keeps)). |
| `@input` on `SendMessageRequest` | Ignored. Smithy's `CLIENT` nullability mode would make every member optional here; this generator keeps `@required` members required (see [Optional members, and `NullableIndex`](#optional-members-and-nullableindex)). |
| `NullableInteger`, `Binary` | `Prelude.Integer` / `Int`, `Prelude.Blob` / `rt.Blob` |
| Map and list shapes | A type alias plus a schema; the runtime's generic codecs handle them |
| Enum shapes | A Scala `enum` with `UnknownValue`, and a string codec |
| The operation's errors | A `TypeRegistry` built at compile time |

The class and the schema carry different names. The Scala field is `queueUrl`, the member is `QueueUrl`. The schema holds the name that goes on the wire.

The generator also dropped traits the runtime never reads. `@xmlFlattened` and `@xmlName` are only used by the XML protocols, so they aren't in the schema.

**Hearth** pairs field 0 (`queueUrl`) with member 0 (`QueueUrl`), field 1 with member 1, and so on. It writes code that calls `writeString(member 0, value.queueUrl)`, skips `delaySeconds` when it's `None`, and calls the nested `MessageAttributeValue` codec for each map entry.

**JSON codec**: takes the name from each member schema:

```json
{"QueueUrl":"http://localhost:4566/000000000000/orders","MessageBody":"hello","DelaySeconds":5,
 "MessageAttributes":{"trace":{"StringValue":"abc","DataType":"String"}}}
```

**Avro codec**: the same calls, but it uses each member's position instead of its name. The result is 80 bytes with no field names in them; 41 of those bytes are the queue URL.

#### DynamoDB `PutItem`: a union, and recursion

**Model**: `AttributeValue` is a **union**. Exactly one member is set: `S`, `N`, `B`, `SS`, `NS`, `BS`, `M`, `L`, `NULL` or `BOOL`. `M` and `L` contain `AttributeValue`s again, so the type is recursive.

**What the generator writes**: a Scala `enum`, one case per member, plus a case for members added later:

```scala
enum AttributeValue {
  case S(value: String)
  case N(value: String)
  ...
  case M(value: Map[String, AttributeValue])   // recursive
  case L(value: List[AttributeValue])          // recursive
  ...
  case UnknownMember(name: String)
}
```

**Hearth** generates a `match`. `AttributeValue.L(xs)` becomes "write the union, member 7 (`L`), as a list", and each element goes back into the same codec, which is how recursion works.

**JSON codec**: a union is an object with exactly one key, the member's name:

```json
{"TableName":"orders","Item":{"id":{"S":"o-1"},"qty":{"N":"2"},"tags":{"L":[{"S":"new"}]}}}
```

**Avro codec**: 48 bytes. A union is written as its member's **index** (`S` = 0, `N` = 1, `L` = 7) followed by the value:

```text
02 0c "orders"            TableName: present, then the 6-byte string
02 06                     Item: present, a map of 3 entries
04 "id"  00 06 "o-1"      key "id"; branch 0 (S); the string "o-1"
06 "qty" 02 02 "2"        key "qty"; branch 1 (N); the string "2"
08 "tags" 0e 02 00 06 "new" 00
                          key "tags"; branch 7 (L); a list of 1: branch 0 (S), "new"; end of list
00                        end of map
00 × 9                    the 9 optional members of PutItemInput that weren't set: null
```

The JSON and Avro outputs come from the same Hearth calls. Only the format codec differs: JSON writes `"S"` where Avro writes `00`.

#### DynamoDB `DescribeTable`: reading a response

The response from DynamoDB, including a field this client doesn't know about:

```json
{"Table":{"TableName":"orders","TableStatus":"ACTIVE","CreationDateTime":1795771800.25,"ItemCount":42,"BrandNewField":true}}
```

**JSON codec (reader)** walks the object. For each key it looks the member up in the schema:
- `TableName`: a string member, so it's passed on as a string.
- `CreationDateTime`: the schema says `Timestamp`, so the number is read as epoch seconds and becomes an `Instant`. That decision belongs to the format codec: awsJson uses epoch seconds.
- `BrandNewField`: no such member in the schema, so it's skipped. Old clients keep working when AWS adds fields.

**Hearth (reading side)** collects the values it's handed into local variables, fills the missing members with `None` or their defaults, and calls the constructor:

```scala
DescribeTableOutput(Some(TableDescription(tableName = Some("orders"), tableStatus = Some(ACTIVE),
  creationDateTime = Some(2026-11-27T09:30:00.250Z), itemCount = Some(42), /* rest */ None ...)))
```

If AWS later sends a status this client has never seen:

```json
{"Table":{"TableName":"orders","TableStatus":"SOME_NEW_STATUS"}}
```

you get `tableStatus = Some(TableStatus.UnknownValue("SOME_NEW_STATUS"))` instead of an error. Smithy enums are open, and the generated enum and its codec are built for that.

#### What each example shows

| | Schema provides | Hearth provides | Format codec provides |
|---|---|---|---|
| SQS `SendMessage` | Wire names (`QueueUrl`), member order, `@required` | Field ↔ member pairing, `Option` handling | `"QueueUrl":` in JSON, a position in Avro |
| DynamoDB `PutItem` | `shapeType = Union`, member index of `L` | `match` from enum case to member, recursion | `{"L":[…]}` in JSON, `0e` in Avro |
| DynamoDB `DescribeTable` | `Timestamp`, which keys are members | Constructor call, `None` for missing, `UnknownValue` | Epoch seconds → `Instant`, skipping unknown keys |

**In one sentence:**
- the **schema** says what the AWS shape looks like;
- **Hearth** knows how to take apart and build *your Scala class* for that shape;
- the **format codec** decides how it looks in *one format*.

Swapping JSON for Avro changes only the last part. Changing the model regenerates the first two.

### Other formats through ZIO Blocks

[ZIO Blocks](https://github.com/zio/zio-blocks) has a codec for many formats. Its codecs work from a ZIO Blocks `Schema[A]`, which our generated types don't have. They also have a codec for its `DynamicValue`, a generic tree of records, variants, sequences, maps and primitives. The bridge in [codecs/](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs) uses that codec:

```scala
// the generated shape codec, unchanged, writes into a DynamicValueWriter:
val value: DynamicValue = codec.toDynamicValue(w => c.write(w, c.schema, table))
// Record(TableName -> "orders", CreationDateTime -> "2026-11-27T09:30:00.250Z", ...)
// then the ZIO Blocks format writes the bytes:
ZSchema[DynamicValue].derive(MessagePackFormat).encode(value)
```

Reading runs the other way: the format decodes a `DynamicValue`, and a `DynamicValueReader` feeds it to the generated codec as `ShapeReader` calls. Nothing in the generator or the derivations changes. A new format is about ten lines in [ZioBlocksCodecs.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/ZioBlocksCodecs.scala).

| Codec | Media type | Module | State on ZIO Blocks 0.0.56 |
|---|---|---|---|
| `ZioBlocksJsonCodec` | `application/json` | `zio-blocks-schema` (JSON is built in) | Round-trips. Plain JSON from ZIO Blocks' writer: no `@jsonName`, no `@timestampFormat`, no `"NaN"` strings. |
| `MessagePackCodec` | `application/msgpack` | `zio-blocks-schema-messagepack` | Round-trips. |
| `AvroCodec` | `application/avro` | `zio-blocks-schema-avro` | Round-trips. The bytes follow ZIO Blocks' fixed Avro schema for `DynamicValue`, not a schema made from the Smithy shape: see [`AvroSchemaCodec`](#avro-for-the-shape-avroschemacodec) for that. |
| `YamlCodec` | `application/yaml` | `zio-blocks-schema-yaml` | Round-trips. Its reader ignores quotes and re-types every scalar, so the string `"007"` is written quoted but comes back as `"7"`. |
| `ThriftCodec` | `application/thrift` | `zio-blocks-schema-thrift` | Writes only. ZIO Blocks can't decode a `DynamicValue` from Thrift without a schema. |

These are left out:

- **XML:** ZIO Blocks' XML codec writes a `DynamicValue` but can't read one. Its element names wouldn't follow `@xmlName`, `@xmlFlattened` or `@xmlAttribute` anyway. Smithy's `restXml` needs the `XmlCodec` placeholder, implemented against those traits.
- **BSON:** its `DynamicValue` codec is "not yet implemented" in 0.0.56.
- **CSV:** it doesn't support `DynamicValue` at all.
- **Protobuf:** ZIO Blocks has no Protobuf module. `ProtobufCodec` stays a placeholder.

The mapping is in [DynamicValueBridge.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/DynamicValueBridge.scala):
- Structures become `Record`s keyed by member name.
- Unions become `Variant`s.
- Maps become `Map`s with string keys.
- Blobs become base64 strings, because `DynamicValue` has no bytes.
- Timestamps become ISO-8601 strings. `PrimitiveValue.Instant` comes back from YAML as the text `Instant(...)`.

The reader is lenient because formats don't keep everything:
- MessagePack writes a variant as `{_case, _value}` and reads every integer back as `Long`.
- YAML reads a variant back as a one-entry record.

None of these codecs is used by a Smithy protocol. `ZioBlocksJsonCodec` writes JSON too, but the protocols stay on `JsonCodec`, because Smithy's JSON rules (`@jsonName`, `@timestampFormat`, `"NaN"` as a string) aren't ZIO Blocks' rules.

### Avro for the shape: `AvroSchemaCodec`

Avro bytes don't describe themselves, even less than Protobuf. Fields have no names and no numbers; they are written one after another in schema order, and a reader always needs the writer's schema. `AvroCodec` round-trips only because ZIO Blocks always uses the same schema, the one for its generic `DynamicValue` tree, so every message spells out its field names and node kinds. Other Avro tools (a schema registry, Kafka consumers, Spark, Avro files) see that generic tree, not a `TableDescription`.

[AvroSchemaCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/AvroSchemaCodec.scala) writes Avro for the shape itself, with Apache Avro 1.12's `BinaryEncoder` and `BinaryDecoder` and no bridge. `AvroSchemas.of(schema)` derives the Avro schema from our runtime `Schema`, so the generator doesn't change:

| Smithy | Avro |
|---|---|
| structure | `record` named after the shape; every field `["null", T]` with default `null` |
| union | `record` with one field `member`: a union of `<Union>_<member>` records with one field `value` |
| list, map | `array`, `map` (`["null", T]` items when `@sparse`) |
| byte, short, integer, intEnum | `int` |
| long, float, double, boolean | the same names |
| string, enum | `string`. Smithy enums are open, and an Avro `enum` would reject a new value. |
| bigInteger, bigDecimal | `string`. Avro's `decimal` needs a fixed scale, and Smithy has none. |
| blob | `bytes` |
| timestamp | `long`, logical type `timestamp-micros` |
| document | `string` holding JSON |

Three of those choices need explaining:
- **Every member is nullable,** even `@required` ones. The writer never has to invent a value, and a schema with an added member can still read old data.
- **Unions are wrapped in a record** because Avro unions have no name. A recursive union like DynamoDB's `AttributeValue` (`M` holds a map of `AttributeValue`) needs a name to refer back to.
- **Writing streams, with no intermediate tree.** Avro has no tags, so the writer fills in a `null` branch for every member the shape codec skips. That only works because both derivations write members in member order; the writer fails if they don't. Reading is the same in reverse: the shape codec must read every member it's handed.

Writer and reader must use the same schema, which means the same model version. Reading data written with an older schema would need Avro's schema resolution (`ResolvingDecoder` with the writer's schema), and this codec doesn't do that. To hand the schema to other tools, print it: `AvroSchemas.of(TableDescription.schema).toString(true)` gives the `.avsc` JSON.

Encoded sizes in bytes, from `clientBench/runMain smithy.scala.bench.Sizes`:

| Format | Small (`GetForecastOutput`) | Large (`Put`, 50 attributes) |
|---|---|---|
| **Avro, shape schema (`AvroSchemaCodec`)** | **68** | **1203** |
| Avro, ZIO Blocks `DynamicValue` (`AvroCodec`) | 174 | 1938 |
| Jackson JSON | 152 | 2259 |
| ZIO Blocks JSON | 188 | 2259 |
| MessagePack | 161 | 2994 |
| Thrift | 156 | 2221 |
| YAML | 184 | 2741 |

ZIO Blocks JSON is 36 bytes larger than Jackson on the small payload. Its three timestamps are ISO strings (`"2026-11-28T00:00:00Z"`), where Jackson writes epoch seconds (`1795737600`). On `Put`, which has no timestamps, the two are the same size.

### Speed: Jackson against ZIO Blocks

Jackson is faster for writing. For reading it wins on small payloads but not on large ones: on the 50-attribute DynamoDB item, ZIO Blocks JSON reads about 12% faster. Times are in microseconds per operation; lower is better.

| Format | Small read | Small write | Large read | Large write |
|---|---|---|---|---|
| **Jackson `JsonCodec`** | **1.36** | **0.77** | 19.2 | **5.9** |
| ZIO Blocks JSON | 2.86 | 0.92 | **16.8** | 9.6 |
| MessagePack | 2.77 | 0.88 | 23.5 | 9.7 |
| Avro | 2.98 | 1.35 | 19.5 | 10.8 |
| YAML | 13.0 | 1.48 | 101.0 | 20.0 |

The small payload is a Weather `GetForecastOutput`. The large one is a DynamoDB `Put` with 50 attributes: strings, numbers, blobs, and nested lists and maps.

- **Writing:** Jackson streams straight from the shape codec to bytes. Each ZIO Blocks codec first builds a `DynamicValue` tree, and on the large item that step alone takes 5.5 µs, more than half of their 9.6 µs. Their own encoders are fast; the bridge's extra tree is the cost.
- **Reading:** our Jackson reader doesn't stream either. It parses the body into a Jackson `JsonNode` tree with `readTree` and then walks it. ZIO Blocks' JSON parser builds its tree faster: about 8.5 µs of its 16.8, the other 8.3 being our reader walking the `DynamicValue`. A streaming Jackson reader would likely win here too, but our error handling reads `__type` from the tree, so that change isn't free.
- **Between the ZIO Blocks formats:** JSON, MessagePack and Avro are close; MessagePack's binary format gives no speed advantage through the bridge. YAML is 5–7 times slower than the others for reading.

`AvroSchemaCodec` against Jackson and the ZIO Blocks Avro codec, from a separate run with two forks (same units):

| Format | Small read | Small write | Large read | Large write |
|---|---|---|---|---|
| **Avro, shape schema (`AvroSchemaCodec`)** | **0.31** | **0.60** | **9.7** | **4.3** |
| Jackson `JsonCodec` | 1.21 | 0.74 | 19.9 | 6.0 |
| Avro, ZIO Blocks `DynamicValue` (`AvroCodec`) | 2.85 | 0.96 | 20.2 | 10.4 |

It's the fastest codec here in all four columns. It reads about twice as fast as Jackson on the large item and four times as fast on the small one, and writes 20–30% faster. There are two reasons:
- **It streams both ways.** There's no `JsonNode` or `DynamicValue` tree on either side.
- **Avro has nothing to parse.** There are no field names to match and no quotes or escapes to scan, only varints and length-prefixed strings in a known order.

The ±0.50 margin on its small write (0.60) means only "about as fast as Jackson" is safe to say there.

These numbers come from one JMH run on an Apple M1 with JDK 25: 3 warmup and 5 measured iterations of 1 second, in one fork. That's enough to rank the codecs but not to separate close results; Avro's write figures have a wide margin (±1.9 µs on 1.35). The benchmarks are in [CodecBench.scala](../../modules/bench/src/main/scala/smithy/scala/bench/CodecBench.scala): `ForecastBench` and `PutBench` per format, and `BridgeBench` for the bridge alone. Run them with:

```
sbt --client "clientBench/Jmh/run -i 5 -wi 3 -f 1"
```

## How the generator reads the model

The example clients are generated from these files:

| Client | Model file | Format |
|---|---|---|
| SQS | `src/main/resources/models/sqs/sqs.json` | Smithy JSON AST, copied from aws/api-models-aws by `scripts/update-aws-models.sh` |
| DynamoDB | `src/main/resources/models/dynamodb/dynamodb.json` | Smithy JSON AST |
| Weather | `modules/client-example/model/weather.smithy` | Smithy IDL 2.0 |

The generator never parses those files itself. [Main.loadModel](../../modules/client-codegen/src/main/scala/smithy/scala/codegen/Main.scala) hands the file to Smithy's own Java `ModelAssembler`:

```scala
Model.assembler(getClass.getClassLoader)
  .discoverModels(getClass.getClassLoader)                 // prelude + trait definitions from the jars
  .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
  .addImport(path)                                         // .json or .smithy
  .assemble()
  .unwrap()                                                // fails on validation errors
```

So the format doesn't matter. The assembler:
- accepts JSON AST and IDL alike;
- validates the model;
- loads the trait definitions it finds on the classpath (`smithy-aws-traits`, `smithy-rules-engine` and the rest);
- returns one semantic `Model`.

The generator works only on that model's shapes, traits and indexes (`Walker`, `OperationIndex`, `ServiceIndex`, `PaginatedIndex`). The same path serves the smithy-build plugin (`scala-client-codegen`). There, smithy-build assembles the model and passes it in through `PluginContext`.

### Knowledge indexes

The assembled `Model` is already a resolved shape graph, not a raw AST:
- shape ids are absolute;
- the prelude is loaded;
- mixins are applied: their members and traits are copied into each shape that uses them.

What the model doesn't hold are answers that need a walk across the graph. "Which operations can this service reach?" is one; "Which errors can this operation return?" is another. Knowledge indexes in `software.amazon.smithy.model.knowledge` precompute those answers. Each implements `KnowledgeIndex`, and `X.of(model)` builds it once per `Model` and caches it there (`Model.getKnowledge`). Calling `of` again is cheap, and every plugin or generator sharing the model shares the index.

| Index | What it answers | Used here |
|---|---|---|
| `TopDownIndex` | `getContainedOperations` / `getContainedResources`: every operation and resource reachable from a service, including operations bound to resources nested inside resources. | Yes: the list of client methods. |
| `OperationIndex` | `getInputShape` / `getOutputShape` (the resolved structures) and `getErrors(operation)`. `getErrors(service, operation)` adds the errors declared on the service. It also answers the reverse question: `getInputBindings`, `getOutputBindings` and `getErrorBindings` give the operations that use a structure. | Yes: input, output and the error registry (`getErrors(service, op)`). |
| `ServiceIndex` | `getProtocols`: the service's traits that are marked `@protocolDefinition`. `getAuthSchemes`: the ones marked `@authDefinition`. `getEffectiveAuthSchemes(service, op)`: what actually applies to one operation once `@auth` and `@optionalAuth` are taken into account. It does **not** resolve errors; that's `OperationIndex`. | Yes: picking the protocol, and each operation's auth schemes. |
| `PaginatedIndex` | `getPaginationInfo(service, op)`: an operation's `@paginated` trait merged with the service's defaults, with the members resolved and their paths followed. | Yes: generated paginators. |
| `HttpBindingIndex` | Request and response bindings (`@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPrefixHeaders`, `@httpPayload`, body). Also the success status code, request and response content types, whether there is a body, and the timestamp format to use for each location. | No: needed for `restJson1` and `restXml`. |
| `NullableIndex` | `isMemberNullable(member, mode)`: whether *generated code* should treat a member as optional. This is about the type in the generated code, not the wire. It takes `@required`, `@default`, `@clientOptional` and `@input` into account, depending on the `CheckMode`. | No (see below). |
| `EventStreamIndex`, `BottomUpIndex`, `IdentifierBindingIndex`, `PropertyBindingIndex` | Event stream members; the path from a shape up to its services; resource identifier and property bindings. | No. |

`Walker` isn't an index. It's in `software.amazon.smithy.model.neighbor`. It walks a shape's closure, and the generator uses it to find every shape to write.

#### Optional members, and `NullableIndex`

The generator decides `Option[A]`, a Scala default or a plain required field with its own rule:
- `@required` without `@clientOptional` gives a required field;
- otherwise a non-null `@default` gives a default value;
- otherwise the field is an `Option`.

That rule is `NullableIndex`'s `SERVER` mode plus `@clientOptional`, and it isn't any built-in mode. It differs from `CLIENT` (what `isMemberNullable(member)` uses) in one place: `CLIENT` makes **every** member of an `@input` structure optional. In the Weather model `input := { @required city: String }` applies `@input`, so under `CLIENT` the generated `GetForecastInput` would take `city: Option[String]`.

`CLIENT` lets a service drop `@required` from an input member later without breaking generated clients. The cost is that callers lose compile-time checks on inputs; this generator keeps the checks. The `CheckMode`s:

| Mode | Optional when |
|---|---|
| `SERVER` | no `@required`, and no non-null `@default` |
| `CLIENT` | as `SERVER`, or the member has `@clientOptional`, or its structure has `@input` |
| `CLIENT_CAREFUL` | as `CLIENT`, or the member targets a structure or union (those can never gain a `@default`) |
| `CLIENT_ZERO_VALUE_V1`, `CLIENT_ZERO_VALUE_V1_NO_INPUT` | Unstable. Only Smithy 1.0-style zero-value defaults count, for generators that predate IDL 2.0. |

`ALLOW_UNKNOWN_TRAITS` is set so that an AWS model using a trait whose definition jar isn't on the generator's classpath still loads. Those traits don't affect the generated code.

The root project's older `src/main/scala/SmithyToScalaCodegen.scala` works the other way: it reads a `model.json` with Jackson, field by field. That approach has no access to trait semantics, the prelude or the service closure.

### Which traits the generator keeps

A model carries far more traits than a client needs at run time: documentation, examples, endpoint rules, and wire traits for protocols the service doesn't use. SQS's `SendMessageRequest` still has `@xmlName` and `@xmlFlattened` from when it spoke awsQuery. The generated schema keeps only what the runtime reads, so the question is how to know which traits those are.

The model itself says which traits a protocol honours. Every protocol trait is declared with `@protocolDefinition(traits: [...])`, and that list names the traits the protocol reads. These are the actual definitions in smithy-aws-traits 1.74.0 and smithy-protocol-traits 1.74.0:

| Protocol | Traits it honours |
|---|---|
| `awsJson1_0`, `awsJson1_1` | `timestampFormat`, `cors`, `endpoint`, `hostLabel` |
| `rpcv2Json`, `rpcv2Cbor` | `cors`, `endpoint`, `hostLabel`, `httpError`, `requestCompression` |
| `restJson1` | the awsJson ones, plus `http`, `httpError`, `httpHeader`, `httpLabel`, `httpPayload`, `httpPrefixHeaders`, `httpQuery`, `httpQueryParams`, `httpResponseCode`, `httpChecksum`, `httpChecksumRequired`, **`jsonName`** |
| `restXml` | the same HTTP bindings, plus **`xmlAttribute`, `xmlFlattened`, `xmlName`, `xmlNamespace`** |
| `awsQuery` | the awsJson ones, plus `awsQueryError` and the four `xml*` traits |
| `ec2Query` | the awsJson ones, plus `ec2QueryName` and the four `xml*` traits |

For example, `awsJson1_0` is declared like this in `aws.protocols.smithy`:

```smithy
@protocolDefinition(
    traits: [timestampFormat, cors, endpoint, hostLabel]
)
@trait(selector: "service")
structure awsJson1_0 with [HttpConfiguration] {}
```

That list explains several choices in this runtime:
- **`@xmlName` and `@xmlFlattened` aren't on awsJson's list,** so an SQS client ignores them.
- **`@jsonName` isn't on awsJson's list either,** so awsJson writes plain member names. That's why `AwsJsonProtocol` uses `JsonSettings(useJsonName = false, …)`. `restJson1` lists it and would honour it.
- **`@timestampFormat` is on awsJson's list but not on rpcv2Json's.** That's why awsJson honours it and rpcv2Json always writes epoch seconds.

The list only covers protocol-specific traits. Some traits are part of the model's meaning, and every client needs them whatever the protocol: `@required`, `@default`, `@sparse`, `@error`, `@retryable`, `@idempotencyToken` and `@paginated`. So the full rule is:

```text
traits to keep = core semantic traits
               ∪ the traits in the service's protocol's @protocolDefinition(traits: [...])
```

The generator used to keep a hand-written list: `jsonName`, `timestampFormat`, `required`, `sparse`, `error`, `httpError`, `retryable` and `idempotencyToken`. It was right for the three protocols supported here, but hard-coded rather than read from the model. It kept `@jsonName` for awsJson services even though `JsonSettings` then ignored it.

It now computes the list from the model, in [ScalaClientGenerator.scala](../../modules/client-codegen/src/main/scala/smithy/scala/codegen/ScalaClientGenerator.scala):

```scala
private val coreTraits: List[ShapeId] =
  List(RequiredTrait.ID, SparseTrait.ID, ErrorTrait.ID, RetryableTrait.ID, IdempotencyTokenTrait.ID)

private val protocolTraits: List[ShapeId] =
  serviceIndex.getProtocols(service).keySet.asScala.toList.sortBy(_.toString).flatMap { protocol =>
    model.getShape(protocol).toScala.flatMap(_.getTrait(classOf[ProtocolDefinitionTrait]).toScala)
      .fold(List.empty[ShapeId])(_.getTraits.asScala.toList)
  }

private val keptTraits: List[ShapeId] = (coreTraits ++ protocolTraits).distinct

private def runtimeTraits(s: Shape): List[(String, String)] =
  keptTraits.flatMap(id => s.findTrait(id).toScala.map(t => id.toString -> traitValue(t)))
```

Then adding `restXml` support would bring in `@xmlName` and `@xmlFlattened` automatically, and an awsJson client would no longer carry `@jsonName`.

How the pieces work:
- **Finding the protocol:** `ServiceIndex.getProtocols` finds the service's protocol traits, such as `aws.protocols#awsJson1_0`.
- **Reading its list:** each protocol trait's own shape carries `ProtocolDefinitionTrait`, and its `getTraits` is the list in the table above.
- **Writing values:** `traitValue` turns each kept trait into the string the runtime's `Traits` map stores. A string trait gives its value, `@httpError(404)` gives `"404"`, an annotation trait such as `@required` gives `""`, and `@retryable(throttling: true)` gives `"throttling"`.
- **Core traits:** `@default` and `@paginated` aren't in the core list, because the generator uses them itself, for Scala default values and paginators, rather than passing them to the runtime.

What changed in the generated example clients (30 files):

| Client | Protocol | Change |
|---|---|---|
| SQS, DynamoDB | `awsJson1_0` | `@httpError` is no longer copied onto error structures; awsJson doesn't list it, and the runtime never read it. `@timestampFormat` stays. |
| Weather | `rpcv2Json` | `@timestampFormat` is no longer copied onto `Day.date`; rpcv2Json doesn't list it and always writes epoch seconds. |

A [generator test](../../modules/client-codegen/src/test/scala/smithy/scala/codegen/ScalaClientGeneratorSpec.scala) checks this on a small awsJson1_0 model: `timestampFormat`, `required` and `error` are kept, while `jsonName`, `xmlName` and `httpError` are dropped.

## What the generator writes

For each shape in the service closure ([Walker](https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/neighbor/Walker.java)), one file in the target package:

| Smithy | Scala |
|---|---|
| `structure` | a `final case class`. `@required` members are plain types, `@default` members get the default value, everything else is an `Option` defaulting to `None`. The companion has `schema` and `given codec = smithy.scala.hearth.HearthCodec.derived[X](schema)` (`rt.ShapeCodec.struct[X](schema)` with `--derivation=mirror`): no serialization code. |
| `@error` structure | the same, extending `rt.ServiceException`; `@retryable` overrides `smithyRetryable` (and `smithyThrottling`). |
| `union` | an `enum` with one case per member, in member order (no fields for `Unit` members), plus `UnknownMember(name)` last. The companion has `schema` and `given codec = smithy.scala.hearth.HearthCodec.derived[X](schema)` (`rt.ShapeCodec.union[X](schema)` with `--derivation=mirror`). |
| `enum`, `intEnum` | an `enum` with a `value`, plus `UnknownValue(raw)`. `knownValues` is `smithy.scala.hearth.HearthCodec.enumValues[X]` (`rt.ShapeCodec.enumValues[X]` with `--derivation=mirror`), `fromValue` looks a value up, and the codec is built from the two. |
| `list`, `map` | a type alias to `List` or `Map[String, _]` (elements are `Option` when `@sparse`), and a companion with just `schema`. The runtime's generic `List` and `Map` codecs read the element schema from the member. |
| An inline `[A]` or `{K: V}` (IDL 2.1) | No file. The assembler turns it into a list or map shape named `_SyntheticListOf__…` and marks it `@synthetic`; members that target it get the plain `List[A]` / `Map[String, V]` type, and the schema is written inline in the `MemberDef`. Member indexes (`1. title`) load as `@smithy.protocols#idx` and need nothing from the generator. |
| `service` | `<SdkId>Client`: a trait with a method per operation and a `<operation>Paginator` per `@paginated` operation, a companion with `ServiceInfo`, one `rt.Operation.of[I, O](id, rt.TypeRegistry.of[...], auth)` per operation, and `apply(config)`. |

Member names become camelCase. Names that are Scala keywords are backticked (`` `type` ``), and names a case class already has get a trailing underscore (`copy_`). A missing `@idempotencyToken` member is filled with a UUID when the input is serialized, so the same token is sent on every retry.

The generated SQS `SendMessageRequest`, for example, is [SendMessageRequest.scala](../../modules/client-example/src/main/scala/example/sqs/SendMessageRequest.scala), and the client is [SQSClient.scala](../../modules/client-example/src/main/scala/example/sqs/SQSClient.scala).

## Scala 3 features in use

The first version of the generator wrote serialization code as text. Every structure, union, list and map got hand-shaped `write` and `read` methods, about 24,000 lines across the three example clients. The second version uses Scala 3 to move that work into the compiler. Generated code now only describes shapes, and the runtime derives everything else.

The sections below describe the runtime's own `Mirror` derivation, which needs nothing beyond the runtime and is what `--derivation=mirror` generates. Generated clients use [the Hearth derivation](#the-hearth-derivation-the-default) by default.

The table is the summary; each feature has a section after it saying why it was used and what it changed.

| Feature | Where | Problem it solved |
|---|---|---|
| [Mirror derivation](#mirror-derivation-for-structures) | `ShapeCodec.struct` | A `write`/`read` pair generated as text for every structure |
| [Sum mirrors](#sum-mirrors-for-unions) | `ShapeCodec.union` | The same for every union, plus pattern matches kept in sync by hand |
| [`summonFrom` and `ValueOf`](#summonfrom-and-valueof-for-enum-values) | `ShapeCodec.enumValues` | A list of enum values written out a second time |
| [A quoted macro](#a-quoted-macro-for-default-values) | `Defaults.of` | Default values duplicated in the reading code |
| [Inline tuple traversal](#inline-tuple-traversal-for-error-registries) | `TypeRegistry.of` | Error registrations as boilerplate lambdas, with no type check |
| [By-name context parameters](#by-name-context-parameters-for-recursive-shapes) | `Option`, `List`, `Map` codecs | Recursive shapes, which derivation alone would loop on |
| [Context bounds with `as`](#context-bounds-with-as) | `Operation.of` | Seven positional constructor arguments per operation |
| [Opaque types](#opaque-types-and-inline-val) | `Traits` | A wrapper object around a `Map` |
| [Named tuples](#named-tuples) | `Headers` | `_1` and `_2`, or `(k, v)` patterns everywhere |
| [Union types](#union-types) | `CallFailure` | A `Throwable` parameter with a catch-all case |
| [`derives CanEqual`](#derives-canequal) | generated enums | Equality under `-language:strictEquality` |

### Mirror derivation for structures

**Why.** A structure codec only needs two things: each field's type and its position. The generator was writing that knowledge out as text. For each structure it emitted one `var` per field, a `match` on member indexes, and the constructor call, for SQS's 100 shapes and DynamoDB's 400 alike. The same template ran hundreds of times, so a template bug became a bug in hundreds of files.

**How.** A `Mirror.ProductOf[A]` gives the field types as a tuple type. `ShapeCodec.struct` walks it with `inline erasedValue[T] match` and `summonInline` to find a codec per field. At runtime, field `i` is written with schema member `i` (`productElement(i)`, `members(i)`) and built back with `mirror.fromProduct`.

**What it changed.** A structure's companion went from about 40 lines to one:

```scala
given codec: rt.SchemaCodec[GetForecastInput] = rt.ShapeCodec.struct[GetForecastInput](schema)
```

The serialization logic now exists once, in [ShapeCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/ShapeCodec.scala), where it's tested directly. If a field type has no codec, compiling the generated file fails, rather than the call failing at runtime.

### Sum mirrors for unions

**Why.** Generated unions had a `write` that matched every case and a `read` that switched on every member index. Both had to agree with the enum's case order, and nothing checked that.

**How.** `Mirror.SumOf[U]` lists the cases in declaration order. For each case, `ShapeCodec.union` takes its name (`constValue[MirroredLabel]`), its value's codec, or none for a `Unit` member, and a constructor. Writing uses the case's `ordinal` as the member index. The generator only has to declare cases in member order, with `UnknownMember` last.

**What it changed.** There are no per-union matches left to fall out of sync. `DerivationSpec` checks the mapping directly: `Kind.Sunny` writes `{"sunny":{}}`, `Kind.Rain(3)` writes `{"rain":3}`, and an unknown member reads as `UnknownMember("hail")`.

### `summonFrom` and `ValueOf` for enum values

**Why.** Every enum listed its values twice: once as cases, and again in a hand-built `knownValues` list. Adding a case meant changing both.

**How.** `ShapeCodec.enumValues[E]` walks the enum's cases. `summonFrom { case v: ValueOf[C] => ... }` keeps the ones that are singletons, which is every case except `UnknownValue(raw)`.

**What it changed.** `knownValues` is now `rt.ShapeCodec.enumValues[Units]`, so the cases are the only list. The test checks that `Units.knownValues == List(METRIC, IMPERIAL)`.

### A quoted macro for default values

**Why.** A `@default` member is a case-class parameter with a default. When it was missing from a response, the old `read` method used a copy of that default, which the generator wrote a second time. `Mirror` exposes field types and names, but not defaults, so derivation alone couldn't do this.

**How.** [Defaults.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Defaults.scala) is a `scala.quoted` macro. It finds the constructor parameters flagged `HasDefault` and calls the companion's synthetic default getters, giving a `Map` from field index to default value.

**What it changed.** Each default exists in one place, the case-class parameter list. The test reads `{"city":"Oslo"}` and gets `units = METRIC, detail = LOW`. It also checks `Defaults.of` directly.

### Inline tuple traversal for error registries

**Why.** Each operation registered its errors with one `new rt.ErrorType(X.schema, r => X.read(r, X.schema))` line per error, 13 lines for SQS `SendMessage`. Nothing stopped a non-error type from being listed.

**How.** `TypeRegistry.of[E1 *: E2 *: EmptyTuple]` walks the tuple type at compile time. For each type it checks `case _: ServiceException`, summons the codec, and otherwise calls `compiletime.error`.

**What it changed.** The registry is one type expression, and a wrong type is a compile error with a clear message. The test confirms this with munit's `compileErrors`: `TypeRegistry.of[String *: EmptyTuple]` fails with "every error type must extend ServiceException".

### By-name context parameters for recursive shapes

**Why.** DynamoDB's `AttributeValue` is a union whose `M` and `L` cases hold maps and lists of `AttributeValue`. To build the `Map` codec you need the `AttributeValue` codec, which is the very thing being built. If either were evaluated eagerly while the other is still being initialized, that would be an initialization loop.

**How.** Two things defer the lookup:

- The `Option`, `List` and `Map` givens take `(using inner: => ShapeCodec[A])` and keep it in a `lazy val`.
- Derived structure and union codecs wrap their field codecs in a thunk.

In both cases, nothing is resolved until the first `write` or `read`, after every companion's `given codec` exists.

**What it changed.** Recursive models compile and run with nothing special in the generator. `DerivationSpec` round-trips a nested `M`-of-`L`-of-`M` value. This was the riskiest part of the change, which is why the DynamoDB client was compiled before anything else. Whether either deferral alone would be enough hasn't been tested. Both are cheap, so both stay.

### Context bounds with `as`

**Why.** `Operation` took seven positional arguments: id, input schema, input writer, output schema, output reader, errors and auth. The four serde arguments were the same pattern every time.

**How.** `Operation.of[I: SchemaCodec as in, O: SchemaCodec as out](id, errors, auth)`. A codec carries its own schema, so the context bounds supply four of the seven arguments. `as` names the evidence without a separate `using` clause.

**What it changed.** Each operation is now one line in the generated client.

### Opaque types and `inline val`

**Why.** `Traits` was a case class wrapping a `Map[String, String]`: an extra object per schema, and an API that leaked `values`.

**How.** `opaque type Traits = Map[String, String]` exposes only `has`, `get` and `++` as extension methods. The trait ids (`Traits.JsonName` and so on) are `inline val`s, so uses compile to string literals.

**What it changed.** There's no wrapper at runtime and nothing extra in the API. Generated code is unaffected: `rt.Traits("smithy.api#required" -> "")` reads the same.

### Named tuples

**Why.** Headers were `Vector[(String, String)]`, read with `(k, v)` patterns or `_1` and `_2`. SigV4 does a lot of header handling, and positional access there is easy to get wrong.

**How.** `Vector[(name: String, value: String)]`. Named tuples are still tuples, so building and matching them works as before.

**What it changed.** Code reads `h.name` and `h.value`. The transport's response-header conversion became a `for` comprehension that yields `(name = ..., value = ...)`.

### Union types

**Why.** `RetryStrategy.isRetryable` took a `Throwable`, so it needed a `case _ => false`. That catch-all would quietly swallow a new kind of failure.

**How.** `type CallFailure = ServiceException | TransportException`. The retry loop's `Either` uses it, its `catch` catches exactly that union, and `isRetryable` matches the two cases with no default.

**What it changed.** The compiler checks that every failure kind is handled. Adding a third kind would turn the match into a warning instead of hiding the gap.

### `derives CanEqual`

**Why.** The build turns on `-language:strictEquality`, so `==` only compiles between types that allow it. Generated enums are compared in `fromValue` and in tests.

**How.** Generated unions and enums say `derives CanEqual`.

**What it changed.** Equality keeps its type checks without casts. Comparing an enum with an unrelated type is still a compile error.

### Costs

- **Errors move.** A missing codec now shows up as a compile error in the generated file that uses it. That's earlier than before, but the message comes out of an inline expansion.
- **Runtime type information is erased.** Derived codecs read fields as `productElement(i)` and keep codecs as `ShapeCodec[Any]`. That means some boxing and casts that the old hand-written code didn't need. The serde tests cover correctness, but nothing here measures speed.
- **The macro needs separate compilation.** `Defaults` lives in the runtime module, compiled before the generated code that expands it.

None of this uses experimental features. The build's strict flags (`-Wvalue-discard`, `-Wnonunit-statement`, `-language:strictEquality`, `-Xcheck-macros`) report nothing for the runtime or the 544 generated files. Overall, the generated sources shrank from about 24,000 lines to about 11,000. In one run here, the example module also compiled in 42 seconds instead of 88.

## The Hearth derivation (the default)

The runtime's own derivation is the `Mirror` one above. [modules/hearth](../../modules/hearth) (sbt project `clientHearth`) holds a second, which generated code uses by default. It is built on [Hearth](https://github.com/kubuszok/hearth), the macro standard library behind [Kindlings](https://github.com/kubuszok/kindlings)' circe and jsoniter derivations. It follows Hearth's [derivation checklist](https://github.com/kubuszok/hearth/blob/master/docs/user-guide/derivation-checklist.md). The runtime doesn't depend on it, but generated code does, so a project using the generated clients depends on `clientHearth`: Hearth and its Cross Quotes compiler plugin come with it.

The generator picks one with its `derivation` setting:

```sh
sbt --client "clientCodegen/runMain smithy.scala.codegen.Main --derivation=mirror <model> <service id> <package> <output dir>"
```

In smithy-build, add `"derivation": "mirror"` to the `scala-client-codegen` plugin settings. The default is `hearth`, and the generated code differs only in what its `given codec`s and `knownValues` call:

| | `mirror` | `hearth` (default) |
|---|---|---|
| Structures | `rt.ShapeCodec.struct[X](schema)` | `smithy.scala.hearth.HearthCodec.derived[X](schema)` |
| Unions | `rt.ShapeCodec.union[X](schema)` | `smithy.scala.hearth.HearthCodec.derived[X](schema)` |
| Enum values | `rt.ShapeCodec.enumValues[X]` | `smithy.scala.hearth.HearthCodec.enumValues[X]` |
| Dependencies | the runtime only | Hearth 0.4.5 and its Cross Quotes compiler plugin |
| Writing a field | `productElement(i)` on a `ShapeCodec[Any]` (boxing, casts) | a direct call, such as `w.writeString(member, value.name)` |
| Reading a structure | an `Array[Any]`, then `Tuple.fromArray` | one typed `var` per field, then the constructor |
| Default values | a separate quoted macro (`Defaults.of`) | Hearth's `Parameter.defaultValue` |
| Recursion | implicit search, by-name givens, runtime thunks | one helper `def` per type, declared before its body |
| Overriding a field type | not possible | a codec in implicit scope wins |
| A type it can't handle | the first missing given, from inside an inline expansion | every failure, each with its reasons, and a hint about logging |
| Derivation log | none | `import smithy.scala.hearth.debug.logDerivation`, or `-Xmacro-settings:smithy.scala.hearth.logDerivation=true` |

`scripts/generate-scala-clients.sh` also generates Mirror-derived copies of the DynamoDB and Weather clients (packages `example.mirror.*`) into `clientExample`'s tests. [ParitySpec](../../modules/client-example/src/test/scala/example/ParitySpec.scala) checks that both derivations write the same JSON and read each other's, for:
- recursive `AttributeValue` items;
- unknown union members;
- missing required members (the same error);
- defaults, sparse lists, `Unit` union members and timestamps.

### How it works

**Why have a second one.** The `Mirror` derivation above works, with the costs it lists. Hearth's checklist describes the alternative a production library would use, and it fixes those costs:
- Every field went through `productElement(i)` on a `ShapeCodec[Any]`, so values were boxed and cast.
- Reads went through `Array[Any]` and `Tuple.fromArray`.
- Recursion relied on implicit search, by-name givens and runtime thunks.
- Default values needed a second, hand-written quoted macro.
- A type with no codec failed with a bare "no given instance" error from deep inside an inline expansion.
- Nothing could override how one field type was written.

**In short.** Generated code still has one line per type, `given codec = smithy.scala.hearth.HearthCodec.derived[X](schema)`. The macro behind it works like this:

- **It walks the type itself.** For each type it tries, in order:
  1. a codec someone provides;
  2. a Smithy simple type;
  3. an `Option`;
  4. a list or map;
  5. a structure;
  6. a union.

  Each rule that doesn't apply records why.
- **Writes and reads are direct calls,** such as `w.writeString(member, value.name)`. The `Mirror` derivation's `productElement`, `ShapeCodec[Any]`, `Array[Any]` and casts don't appear. Reads use one typed `var` per field and a single pass over the input.
- **Each type is derived once.** It gets a helper `def` per direction, declared before its body, so recursive types call their own helper. No implicit-search, by-name or thunk workaround is needed.
- **Users can override any field type.** A codec in scope is used instead of deriving one. The library's own fallback instances are skipped, and so is the type being derived.
- **Default values come from Hearth's case-class support,** so it doesn't need the `Mirror` derivation's separate `Defaults` macro.
- **Errors are reported together.** Every failing field or case appears in one compile error, each with its reason and a hint about logging.

The details:

[HearthCodecMacrosImpl.scala](../../modules/hearth/src/main/scala/smithy/scala/hearth/internal/HearthCodecMacrosImpl.scala) is one Hearth macro, a Scala 3 adapter over shared logic, written with Hearth's Cross Quotes (`Expr.quote` / `Expr.splice`):

- **It walks the type itself.** For each type it tries rules in order:
  1. a codec someone provides (`Expr.summonImplicitIgnoring`);
  2. a Smithy simple type, written with a direct call such as `w.writeString(member, value)`;
  3. an Option (`IsOption`);
  4. a list or a map (`IsCollection` and `IsMapOf`, whose builders also do the reading);
  5. a structure (`CaseClass`, with `caseFieldValuesAt` to write and `parConstruct` to build);
  6. a union (`Enum.parMatchOn`).

  Each rule that doesn't apply says why.
- **One helper `def` per type and direction.** `write_X(w, s, v)` and `read_X(r, s)` live in a `ValDefsCache` and are declared before their bodies are derived. Each type is derived once, and a recursive type calls its own helper.
- **Provided codecs come first.** The generated `given codec` of a nested type, or one a user writes, is used as is and cached in a `lazy val` of the instance. The library's own fallback givens are excluded from that search, and so is the type being derived.
- **Reading builds no temporary collections.** It declares one typed `var` per field (`ValDefs.createVar`), runs a single `readStruct`, and calls the constructor with `if (seen) value else <missing>`.
- **Missing members.** A missing member gets, in order: its default value (Hearth reads the case class's default getters from `Parameter.defaultValue`); `None`; an empty collection; a simple type's zero; a provided codec's `absent`; or a `SerializationException` naming it.
- **Errors are typed and collected.** Fields and cases are derived with `parTraverse`, so every failure is reported in one compile error. The error is a small ADT, rendered once by `runToExprOrFail`, with a hint about logging.
- **The log is opt-in.** It's switched on by `import smithy.scala.hearth.debug.logDerivation` or `-Xmacro-settings:smithy.scala.hearth.logDerivation=true`.
- **Instances come from a factory.** They're built through `HearthCodec.instance(schema, write, read, absent)` from lambdas, not one anonymous class per type.

`HearthCodec.enumValues[E]` is a second, small macro on the same base. It lists an enum's singleton cases (`Expr.singletonOf`), which is every value but `UnknownValue(raw)`.

**What it changed.**

- **Generated code is still one line per type:** `given codec: rt.SchemaCodec[X] = smithy.scala.hearth.HearthCodec.derived[X](schema)`. Default values need no separate macro.
- **Writes and reads are direct calls**, with no `productElement`, no `Array[Any]` and no casts.
- **Users can override a field type.** [HearthDerivationSpec](../../modules/hearth/src/test/scala/smithy/scala/hearth/HearthDerivationSpec.scala) gives `Celsius` its own codec (written as `"21.5C"`), and the derived `Reading` codec uses it.
- **Recursive case classes need nothing special.** `case class Tree(name: String, children: List[Tree])` derives as is, and round-trips.
- **Errors arrive all at once**, each with its reason:

  ```text
  HearthCodec.derived failed for smithy.scala.hearth.HearthDerivationSpec.Unsupported:
  scala.collection.immutable.Map[scala.Int, java.lang.String]: Smithy maps have String keys, but this one has scala.Int
  No rule could derive a codec for java.util.UUID:
    - not a provided ShapeCodec: none in implicit scope
    - not a Smithy simple type: not one of them
    - not an Option: not an Option
    - not a list or a map: not a collection
    - not a structure: java.util.UUID is not a case class
    - not a union: java.util.UUID is not sealed, not an enumeration, and not a union type
  See how it was derived with: import smithy.scala.hearth.debug.logDerivation, or the scalac option -Xmacro-settings:smithy.scala.hearth.logDerivation=true
  ```

- **The log shows every decision**, for example:

  ```text
  ├ field temperature: smithy.scala.hearth.HearthDerivationSpec.Celsius:
  │ └ write smithy.scala.hearth.HearthDerivationSpec.Celsius:
  │   ├ [Info]  Using the provided smithy.scala.runtime.ShapeCodec[smithy.scala.hearth.HearthDerivationSpec.Celsius]
  │   └ [Info]  smithy.scala.hearth.HearthDerivationSpec.Celsius: handled as a provided ShapeCodec
  ```

- **Methods stay small.** Of 17,081 methods in the compiled SQS and DynamoDB companions, none exceeds the 8,000-byte limit above which HotSpot won't JIT-compile a method. The largest, `TableDescription`'s read helper, is 1,260 bytes.

### Costs of the Hearth derivation

- **A dependency and a compiler plugin.** Hearth 0.4.5 and its Cross Quotes plugin are built for Scala 3.3.9, and Hearth's own CI runs them on Scala 3.9.0, the version this build uses. A plugin depends on compiler internals, so a compiler upgrade can need a Hearth upgrade. Keeping it in its own module keeps that out of the runtime.
- **Captured variables.** The read side's per-field `var`s are captured by the `readStruct` callback, so Scala boxes each in a small `ObjectRef` or `BooleanRef` per call. Nothing here measures throughput.
- **Expansion time.** Each derivation runs in Hearth's `MIO` with a 30-second timeout. In two clean builds, the 544 Hearth-derived example files compiled in 19 and 11 seconds; the `Mirror` version took 42 in the earlier measurement.

## Using a generated client

```scala
import java.net.URI
import smithy.scala.runtime.*
import example.sqs.*

val sqs = SQSClient(
  ClientConfig(
    region = "us-east-1",
    endpoint = Some(URI.create("https://127.0.0.1:9297")), // omit for https://sqs.<region>.amazonaws.com
    credentials = CredentialsProvider.environment
  )
)

val sent = sqs.sendMessage(SendMessageRequest(queueUrl = "https://127.0.0.1:9297/000000000000/orders", messageBody = "hello", delaySeconds = Some(5)))

try sqs.sendMessage(SendMessageRequest(queueUrl = "missing", messageBody = "x"))
catch { case e: QueueDoesNotExist => println(e.smithyMetadata.flatMap(_.requestId)) }

val allQueues = sqs.listQueuesPaginator(ListQueuesRequest()).flatMap(_.queueUrls.getOrElse(Nil)).toList
```

To talk to the [AWS Emulator](../aws-emulator/README.md), pass `transport = JavaHttpTransport(sslContext = Some(ctx))` with an `SSLContext` that trusts its certificate.

## Tests

| Test | What it checks |
|---|---|
| [SigV4Spec](../../modules/client-runtime/src/test/scala/smithy/scala/runtime/SigV4Spec.scala) | Signatures from the AWS SigV4 test suite (`get-vanilla`, `post-vanilla`, `get-vanilla-query-order-key`) and session tokens. |
| [JsonCodecSpec](../../modules/client-runtime/src/test/scala/smithy/scala/runtime/JsonCodecSpec.scala) | `@jsonName` and `@timestampFormat` per protocol settings, `NaN`, base64 blobs, skipping unknown fields. |
| [ScalaClientGeneratorSpec](../../modules/client-codegen/src/test/scala/smithy/scala/codegen/ScalaClientGeneratorSpec.scala) | Generator output for a small model, rejection of unsupported protocols, and a run through `SmithyBuild` as a plugin. |
| [HearthDerivationSpec](../../modules/hearth/src/test/scala/smithy/scala/hearth/HearthDerivationSpec.scala) | The Hearth derivation: a user-provided codec wins, a recursive case class derives as is, and an unsupported type reports every reason in one error. |
| [ParitySpec](../../modules/client-example/src/test/scala/example/ParitySpec.scala) | Both derivations on the same generated types (the default Hearth clients and their Mirror copies) write the same JSON and read each other's. |
| [DerivationSpec](../../modules/client-example/src/test/scala/example/DerivationSpec.scala) | The derived codecs on the generated clients: default values, error correction, enum values, union ordinals, recursive round-trips, and the compile error from `TypeRegistry.of`. |
| [AvroSchemaCodecSpec](../../modules/client-example/src/test/scala/example/AvroSchemaCodecSpec.scala) | Avro for the shape: round-trips (recursive unions, `@sparse` nulls, microsecond timestamps), the derived Avro schema, Apache Avro's `GenericDatumReader` reading our bytes and our reader reading `GenericDatumWriter` output, and sizes against the other codecs. |
| [FormatBridgeSpec](../../modules/client-example/src/test/scala/example/FormatBridgeSpec.scala) | Generated DynamoDB and Weather shapes written as ZIO Blocks JSON, MessagePack, Avro and YAML and read back. Also checks the JSON text, Thrift writing only, and the `DynamicValue` shape. |
| [GeneratedClientSpec](../../modules/client-example/src/test/scala/example/GeneratedClientSpec.scala) | The generated SQS, DynamoDB and Weather clients against a local server: wire format, signing, modelled and unknown errors, retries, paginators, recursive unions, idempotency tokens, `rpcv2Json` paths and Unit input. |

Run them all with `sbt --client "clientRuntime/test; clientCodegen/test; clientExample/test; clientHearth/test"`.

## What it leaves out

- **Protocols:** only `awsJson1_0`, `awsJson1_1` and `rpcv2Json`. The generator refuses other services and names their protocol, so S3, IAM or Lambda need `restXml`, `awsQuery` or `restJson1` support first.
- **Endpoints:** no rules engine. An explicit endpoint wins; otherwise `https://<endpointPrefix>.<region>.amazonaws.com`. FIPS, dual-stack and partitions other than `aws` aren't handled.
- **Credentials:** static and environment variables only; no profiles, SSO or instance metadata.
- **Streaming:** no event streams (such operations are skipped) and no streaming bodies; blobs are held in memory.
- **API style:** calls are blocking. They run well on virtual threads, but there is no async or effect-typed API.
- **Not generated:** waiters, client-side validation beyond `@required` being a non-optional type, and endpoint discovery (DynamoDB doesn't need it).
- **Error correction:** a required member missing from a response gets a zero value, as the Smithy spec suggests. A missing required structure is the exception: it fails deserialization.

For production Scala clients, [smithy4s](https://disneystreaming.github.io/smithy4s/) is the established generator; it's already in this playground's `project/plugins.sbt`.

Back to [the overview](README.md).
