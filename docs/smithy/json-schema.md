# JSON Schema from Smithy

JSON Schema is a vocabulary for describing JSON documents: which properties an object has, their types, which are required, and constraints such as patterns, lengths and ranges. A validator takes a schema and a document, and says whether the document fits. That's all it does: it doesn't say how the document travels, and it knows nothing about operations or errors. In the [three jobs](http-and-formats.md#format-schema-and-protocol-three-separate-jobs) of data format, schema and protocol, it's a schema, for one data format, JSON.

This repo exports a JSON Schema for any Smithy shape with [EventSchemas.scala](../../modules/client-codegen/src/main/scala/smithy/scala/codegen/EventSchemas.scala), and [EventSchemasSpec](../../modules/client-example/src/test/scala/example/EventSchemasSpec.scala) checks that what `JsonCodec` writes passes it. The companion page, [Avro schemas from Smithy](avro-schema.md), does the same for Avro.

## Why you'd need one when you have Smithy

Smithy already describes your data more precisely than JSON Schema can. A JSON Schema is for the people and tools that don't read Smithy:

| Who | Why they want a JSON Schema |
|---|---|
| **Event consumers** in other teams and languages | A Kafka topic carries JSON events. Consumers in Python, Go or Node.js validate or generate types from a JSON Schema; they won't install Smithy tooling. |
| **A schema registry** | Confluent Schema Registry, AWS Glue Schema Registry and Apicurio accept JSON Schema, and check each new version for compatibility before producers may use it. |
| **Webhook receivers** | Partners receiving your webhooks validate payloads, and generate types, from a published schema. Smithy has no webhook concept ([Building a fintech on Smithy](fintech.md#7-one-model-several-protocols)). |
| **Event catalogues** | AsyncAPI documents describe message payloads with JSON Schema (or Avro). |
| **Data platforms** | Ingestion pipelines validate JSON records before they land in a lake or warehouse, and reject bad ones early. |
| **LLM tool calling** | Function and tool definitions for language models take a JSON Schema for the arguments. |
| **OpenAPI** | OpenAPI's `components.schemas` are JSON Schema (fully so in OpenAPI 3.1). Smithy's [OpenAPI conversion](fintech.md#7-one-model-several-protocols) uses the same converter as this page. |

So the model stays the source of truth, and the JSON Schema is one more artifact generated from it, like the client SDKs.

## How Smithy shapes become JSON Schema

Smithy ships a converter, `smithy-jsonschema` (`JsonSchemaConverter`). Its main mappings:

| Smithy | JSON Schema |
|---|---|
| `structure` | `"type": "object"` with `properties`, and `required` from `@required` members |
| `union` | `oneOf`, each a one-property object: `{"rain": 3}` |
| `list` | `"type": "array"` with `items` |
| `map` | `"type": "object"` with `additionalProperties` (or `propertyNames` for constrained keys) |
| `enum` | `"type": "string"` with an `enum` list |
| `intEnum` | `"type": "integer"` with an `enum` list |
| `byte`, `short`, `integer`, `long` | `"type": "number"`, or `"integer"` with `useIntegerType` |
| `float`, `double`, `bigDecimal` | `"type": "number"` |
| `timestamp` | `"type": "number"` for epoch seconds, or `"type": "string"` with `"format": "date-time"` |
| `blob` | `"type": "string"` (base64) |
| `document` | No constraints: any JSON value |
| `@length`, `@pattern`, `@range` | `minLength` / `maxLength` (or `minItems` / `maxItems`), `pattern`, `minimum` / `maximum` |
| `@documentation` (`///` comments) | `description` |

Shapes used by more than one member go under `definitions` and are referenced with `$ref`.

## A JSON Schema is only right if it matches the serializer

A schema describes bytes someone writes. If it describes different bytes from the ones your codec produces, valid events fail validation, or invalid ones pass. The converter's defaults follow Smithy's general JSON conventions, not a particular codec, and running real `JsonCodec` output through a validator found four places where they differ:

| What differs | The converter's default | What `JsonCodec` writes | How `EventSchemas` matches it |
|---|---|---|---|
| `@timestampFormat` | Honoured: `date-time` becomes a string | rpcv2Json ignores it and always writes epoch seconds; awsJson honours it | Takes the protocol's `JsonSettings`, and removes `@timestampFormat` from the model when the protocol ignores it |
| `@sparse` lists and maps | Ignored: items can't be `null` | `null` entries | A schema mapper (`SparseNulls`) wraps items in `anyOf: [item, {"type": "null"}]` |
| Integers | `"type": "number"`, so `10.5` passes for a `long` | Whole numbers | `useIntegerType` |
| `NaN` and the infinities | Not allowed | The strings `"NaN"`, `"Infinity"`, `"-Infinity"` | `supportNonNumericFloats` |

A fifth problem isn't about the bytes: the converter checks for name clashes across the whole model it's given. A model loaded with `discoverModels` also holds Smithy's own shapes, such as `smithy.rules#Condition`, which clashes with DynamoDB's `Condition` under `#/definitions/Condition` and fails the conversion. `EventSchemas` gives the converter a model of only the shapes the event uses (found with `Walker`).

The converter also omits `$schema`, so `EventSchemas` adds `"$schema": "http://json-schema.org/draft-07/schema#"`. Registries and validators use it to pick the dialect.

## Example: a `PaymentSettled` event

An event from the [fintech model](fintech.md), reusing its `Money`, `PaymentId` and `PaymentStatus` shapes:

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

`schemas/PaymentSettled.schema.json`:

```json
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "required": [
        "amount",
        "eventId",
        "paymentId",
        "settledAt",
        "status"
    ],
    "properties": {
        "amount": {
            "$ref": "#/definitions/Money"
        },
        "eventId": {
            "type": "string"
        },
        "fee": {
            "$ref": "#/definitions/Money"
        },
        "paymentId": {
            "type": "string",
            "pattern": "^pay_[A-Za-z0-9]{20}$"
        },
        "settledAt": {
            "type": "number"
        },
        "status": {
            "$ref": "#/definitions/PaymentStatus"
        }
    },
    "description": "Published to the payments.settled topic when a payment settles.",
    "definitions": {
        "Money": {
            "type": "object",
            "required": [
                "amountMinor",
                "currency"
            ],
            "properties": {
                "amountMinor": {
                    "type": "integer"
                },
                "currency": {
                    "type": "string",
                    "pattern": "^[A-Z]{3}$",
                    "description": "An ISO 4217 currency code, such as EUR or USD."
                }
            },
            "description": "An amount of money in minor units: 1050 with EUR is 10.50 EUR, 1050 with JPY is 1050 JPY.\nThe number of decimals per currency comes from ISO 4217."
        },
        "PaymentStatus": {
            "type": "string",
            "enum": [
                "PENDING",
                "SETTLED",
                "FAILED",
                "REFUNDED"
            ]
        }
    }
}
```

What to notice:
- **`required`** lists exactly the `@required` members; `fee` is optional.
- **`settledAt`** is a number: epoch seconds, because the default settings are rpcv2Json's. Pass `--json=awsJson` for awsJson, which honours `@timestampFormat`.
- **`amountMinor`** is an `integer`, so `10.5` is rejected for an amount in cents.
- **`paymentId`** and **`currency`** keep their `@pattern`, and the `///` comments became `description`s.
- **`Money`** and **`PaymentStatus`** are under `definitions`, because they're shapes of their own.
- There's no `"additionalProperties": false`, so an event with a member added later still validates against this version. That's what lets producers add optional members without breaking consumers.

## Evolving the schema

| Change to the Smithy shape | Old events against the new schema | New events against the old schema |
|---|---|---|
| Add an optional member | Pass | Pass (no `additionalProperties: false`) |
| Add a `@required` member | Fail: old events lack it | Pass |
| Remove a member | Pass | Pass, unless it was required in the old schema |
| Add an enum value | Pass | **Fail**: the old schema's `enum` list doesn't have it |
| Tighten a `@pattern`, `@length` or `@range` | May fail | Pass |

Adding an enum value is the surprising one. Smithy enums are open, and generated clients keep unknown values (this repo's generated enums have an `UnknownValue(raw)` case), but a JSON Schema `enum` is closed: a consumer validating with the old schema rejects the new value. [EventSchemasSpec](../../modules/client-example/src/test/scala/example/EventSchemasSpec.scala) checks this. Either roll consumers forward before producers send the new value, or post-process the schema to replace `enum` with a plain `"type": "string"` for enums you expect to grow. Avro avoids it here by writing enums as strings ([Avro schemas from Smithy](avro-schema.md#how-smithy-shapes-become-avro)).

## How it's tested

[EventSchemasSpec](../../modules/client-example/src/test/scala/example/EventSchemasSpec.scala) exports schemas for Weather's `GetForecastOutput` (rpcv2Json: a union with a `Unit` member, a sparse list, a `date-time` timestamp) and DynamoDB's `TableDescription` and `Put` (awsJson: enums, longs, timestamps, the recursive `AttributeValue` union). It then:
- writes values with `JsonCodec`, set up as each protocol sets it up, and validates them with [networknt json-schema-validator](https://github.com/networknt/json-schema-validator) (draft-07): no errors;
- checks that the schema rejects a missing required member, a string where an integer belongs, a `date-time` string where epoch seconds belong, `10.5` for a `long`, and an unknown enum value.

## Further reading

- [JSON Schema: understanding JSON Schema](https://json-schema.org/understanding-json-schema/)
- [Smithy: converting to JSON Schema and OpenAPI](https://smithy.io/2.0/guides/model-translations/converting-to-openapi.html), which lists the converter's settings
- [Avro schemas from Smithy](avro-schema.md), [Building a fintech on Smithy](fintech.md), [Schemas in Scala](schema.md)
