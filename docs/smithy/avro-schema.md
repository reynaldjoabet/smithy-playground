# Avro schemas from Smithy

[Apache Avro](https://avro.apache.org/) is a binary data format in which the schema isn't optional. The bytes hold values only: no field names, no type tags, no lengths for fixed-size values. A reader can't make sense of them without the schema they were written with. In return, Avro is compact and fast, and it has precise rules for reading data written with an older or newer version of the schema. Its schemas are JSON documents, usually saved as `.avsc` files.

This repo writes Avro with [AvroSchemaCodec](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/AvroSchemaCodec.scala) ([Avro for the shape](scala-client.md#avro-for-the-shape-avroschemacodec)), and exports the matching `.avsc` for any Smithy shape with [EventSchemas.scala](../../modules/client-codegen/src/main/scala/smithy/scala/codegen/EventSchemas.scala). The companion page, [JSON Schema from Smithy](json-schema.md), does the same for JSON.

## Why you'd need one when you have Smithy

Because Avro readers *need* a schema, an Avro schema isn't documentation: nobody can read your events without it. Smithy describes the data, but the tools that read Avro don't read Smithy:

| Who | Why they want an Avro schema |
|---|---|
| **Kafka consumers** | High-volume topics use Avro for its size and speed. Consumers fetch the writer's schema from a schema registry and decode with it, in any language. |
| **A schema registry** | Confluent Schema Registry, AWS Glue Schema Registry and Apicurio store Avro schemas, give each version an id, and reject a new version that breaks the topic's compatibility mode (`BACKWARD`, `FORWARD`, `FULL`). |
| **Data platforms** | Spark, Flink, Hive, Iceberg and most lake ingestion tools read Avro natively, and map it to columnar formats such as Parquet. |
| **Long-term storage** | An Avro container file carries its schema in its header, so a file written today can still be read in ten years without the code that wrote it. |
| **Change data capture** | Debezium and similar tools can publish database changes as Avro, so events from your services and from your databases land in one format. |

## How Smithy shapes become Avro

There is no standard mapping from Smithy to Avro, so this repo defines one, in `AvroSchemas.of` ([AvroSchemaCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs/AvroSchemaCodec.scala)):

| Smithy | Avro | Why |
|---|---|---|
| `structure` | `record` named after the shape; every field `["null", T]` with default `null` | So writers never invent values, and fields can be added or removed without breaking readers |
| `union` | `record` with one field `member`, a union of `<Union>_<member>` records | Avro unions are unnamed; a recursive union (DynamoDB's `AttributeValue`) needs a name to refer back to |
| `list` / `map` | `array` / `map`, with `["null", T]` items when `@sparse` | |
| `boolean`, `string`, `blob` | `boolean`, `string`, `bytes` | Blobs are raw bytes, not base64 |
| `byte`, `short`, `integer`, `intEnum` | `int` | |
| `long`, `float`, `double` | `long`, `float`, `double` | |
| `bigInteger`, `bigDecimal` | `string` | Avro's `decimal` needs a fixed scale; Smithy's `bigDecimal` has none |
| `enum` | `string` | Smithy enums are open; an Avro `enum` would reject values added later |
| `timestamp` | `long` with logical type `timestamp-micros` | |
| `document` | `string` holding JSON | |

## One mapping, two entry points

The same `.avsc` can come from two places:

- **At run time, from generated code:** `AvroSchemas.of(PaymentSettled.schema)`. This is the schema `AvroSchemaCodec` writes with.
- **At build time, from the model:** `EventSchemas.avroSchema(model, id)`, which needs no generated code, so CI can register a schema before any service is built.

The build-time path doesn't reimplement the mapping. [ModelSchemas.scala](../../modules/client-codegen/src/main/scala/smithy/scala/codegen/ModelSchemas.scala) builds the runtime's `Schema` straight from the Smithy model (the same schema the generator writes into each companion), and passes it to the same `AvroSchemas.of`. So the exported schema can't drift from what the codec writes, and the tests check that the two paths give equal schemas.

## Example: a `PaymentSettled` event

The event from [JSON Schema from Smithy](json-schema.md#example-a-paymentsettled-event):

`model/events.smithy`:

```smithy
$version: "2.1"

namespace com.example.events

use com.example.money#Money
use com.example.payments#PaymentId
use com.example.payments#PaymentStatus

/// Published to the payments.settled topic when a payment settles.
structure PaymentSettled {
    @required
    eventId: String

    @required
    paymentId: PaymentId

    @required
    status: PaymentStatus

    @required
    amount: Money

    /// The fee charged, if any.
    fee: Money

    @required
    settledAt: Timestamp
}
```

Exported with:

```sh
sbt --client "clientCodegen/runMain smithy.scala.codegen.EventSchemas model/ schemas/ com.example.events#PaymentSettled"
```

`schemas/PaymentSettled.avsc`:

```json
{
  "type" : "record",
  "name" : "PaymentSettled",
  "namespace" : "com.example.events",
  "fields" : [ {
    "name" : "eventId",
    "type" : [ "null", "string" ],
    "default" : null
  }, {
    "name" : "paymentId",
    "type" : [ "null", "string" ],
    "default" : null
  }, {
    "name" : "status",
    "type" : [ "null", "string" ],
    "default" : null
  }, {
    "name" : "amount",
    "type" : [ "null", {
      "type" : "record",
      "name" : "Money",
      "namespace" : "com.example.money",
      "fields" : [ {
        "name" : "amountMinor",
        "type" : [ "null", "long" ],
        "default" : null
      }, {
        "name" : "currency",
        "type" : [ "null", "string" ],
        "default" : null
      } ]
    } ],
    "default" : null
  }, {
    "name" : "fee",
    "type" : [ "null", "com.example.money.Money" ],
    "default" : null
  }, {
    "name" : "settledAt",
    "type" : [ "null", {
      "type" : "long",
      "logicalType" : "timestamp-micros"
    } ],
    "default" : null
  } ]
}
```

What to notice:
- **Every field is `["null", T]` with `"default": null`, even `@required` ones.** Avro can't express "required", so the schema doesn't try. Required members are enforced where values are made and read: the generated case classes can't be built without them, and the shape codec fails on a missing one.
- **`Money` is defined once**, in its own namespace (`com.example.money`), and `fee` refers to it by full name.
- **`status` is a `string`**, not an Avro `enum`, so adding `CHARGEBACK` to `PaymentStatus` later doesn't break old consumers.
- **`settledAt` is `timestamp-micros`**: a `long` count of microseconds since the epoch.
- **`amountMinor` is a `long`**, exact for amounts in cents.

## Evolving the schema

Avro readers resolve the writer's schema against their own, matching fields by name. With every field nullable and defaulting to `null`:

| Change to the Smithy shape | New reader, old data | Old reader, new data |
|---|---|---|
| Add a member (optional or `@required`) | Works: the field is `null` | Works: the field is skipped |
| Remove a member | Works: the field is skipped | Works: the field is `null` |
| Add an enum value | Works: it's a string | Works |
| Rename a member | **Breaks**: Avro matches by name, and no `aliases` are generated | **Breaks** |
| Change a member's type | **Breaks** (except Avro's promotions, such as `int` to `long`) | **Breaks** |
| Add a union member | Works | **Breaks**: the old reader doesn't know the new branch |

The first three rows are why every field is nullable: under the registry's `FULL` mode, which needs both directions, adding and removing members is always allowed. A `@required` member that a new reader gets as `null` from old data fails in the shape codec, not in Avro, so adding a required member is still a breaking change for your code; `smithy diff` and the registry won't stop it.

## Using it with a schema registry

1. **Register in CI.** Export the `.avsc` from the model and register it under the topic's subject (for Confluent, `<topic>-value`). The registry rejects it if it breaks the subject's compatibility mode, so a breaking change fails the build before any producer ships.
2. **Prefix each message.** Confluent's wire format puts five bytes before the Avro data: a zero "magic" byte and the 4-byte schema id. `AvroSchemaCodec` writes plain Avro binary, without that header, so a producer adds it, and a consumer strips it and fetches the schema by id. AWS Glue Schema Registry uses its own header.
3. **Read with the writer's schema.** Consumers decode with the schema the message was written with, and resolve it against their own reader schema, which gives the evolution rules above.

## How it's tested

[EventSchemasSpec](../../modules/client-example/src/test/scala/example/EventSchemasSpec.scala), on Weather's `GetForecastOutput` and DynamoDB's `TableDescription` and `Put`:
- **The two entry points agree:** the schema exported from the model equals `AvroSchemas.of` on the generated companion's schema.
- **Apache Avro reads our bytes:** values written by `AvroSchemaCodec` are decoded by Avro's own `GenericDatumReader`, with only the exported `.avsc` and no code from this repo. That includes the recursive `AttributeValue` union, a sparse list holding a `null`, and a timestamp read back as microseconds.

## Further reading

- [Apache Avro specification](https://avro.apache.org/docs/current/specification/), especially "Schema Resolution"
- [Confluent: schema evolution and compatibility](https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html)
- [JSON Schema from Smithy](json-schema.md), [A Scala client generator: Avro](scala-client.md#avro-for-the-shape-avroschemacodec), [Building a fintech on Smithy](fintech.md)
