# Building a fintech on Smithy

A fintech's API is a contract with money attached. A field that silently loses a cent, a retry that pays twice, an IBAN that ends up in a log file, or a "small" change that breaks a partner's integration each cost real money or a regulator's attention. Smithy helps because the contract is a model that tools can check, not a document people have to remember. You write the API once, encode your rules as traits and validators, and let the build enforce them on every change, before any code exists.

This page shows how, with a payments service as the running example. Every model on this page was validated with Smithy 1.75.1 on 2026-11-28. The validators were checked against a model that breaks each rule, the diff example was run with `smithy-diff`, the service was converted to OpenAPI, and the repo's own generator produced a Scala client from it ([What this repo can do with it today](#what-this-repo-can-do-with-it-today)).

## What Smithy gives a fintech

| Fintech need | What Smithy offers | Section |
|---|---|---|
| Amounts that never lose precision | Model money once as a shape, and ban `float` and `double` with a validator | [Money](#1-model-money-once) |
| A retry must never pay twice | `@idempotencyToken`, which clients fill in, plus `@idempotent` and `@readonly` on operations | [Idempotency](#2-idempotency-and-retries) |
| Errors partners can handle in code | Typed errors with `@error`, `@httpError` and `@retryable` | [Errors](#3-errors-are-part-of-the-contract) |
| PII and card data kept under control | Custom traits (`@pii`, `@pciData`) and Smithy's `@sensitive` | [Compliance](#4-compliance-as-traits-checked-by-the-build) |
| House rules nobody can forget | `metadata validators` with selectors: a broken rule fails the build | [Compliance](#4-compliance-as-traits-checked-by-the-build) |
| Never break a partner | `smithy diff` in CI against `main` | [Compatibility](#6-compatibility-smithy-diff-in-ci) |
| REST for partners, fast RPC inside | One model, a protocol trait per service | [Protocols](#7-one-model-several-protocols) |
| SDKs and docs that match the API | Code generators for many languages, OpenAPI conversion, docgen | [Code generation](#8-generating-servers-and-clients) |
| A data inventory for GDPR | `smithy select` lists every member with a given trait | [Compliance](#a-data-map-for-free) |

## The shape of it

```text
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Model repo: payments, accounts and vault services, compliance traits,           │
│ validators, one namespace per domain                                            │
└─────────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ CI: smithy build  (validators: any DANGER event fails the build)                │
│     smithy diff --mode git  (breaking changes against main fail the PR)         │
└─────────────────────────────────────────────────────────────────────────────────┘
                                         │
          ┌────────────────────┬─────────┴──────────┬────────────────────┐
          ▼                    ▼                    ▼                    ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ Server stubs     │ │ Clients          │ │ OpenAPI          │ │ Event schemas    │
│ smithy4s,        │ │ internal and     │ │ partner portal,  │ │ JSON Schema,     │
│ smithy-java,     │ │ partner SDKs     │ │ API gateway      │ │ Avro for Kafka   │
│ smithy-rs        │ │                  │ │                  │ │                  │
└──────────────────┘ └──────────────────┘ └──────────────────┘ └──────────────────┘
```

One repository holds every model. CI is the gatekeeper: a change that breaks a house rule, or breaks compatibility with what's on `main`, doesn't merge. Everything below the gate is generated from the same model, so the server, the SDKs, the partner docs and the event schemas can't drift apart.

## 1. Model money once

Money is the first thing to get right, and the easiest to get wrong. `float` and `double` can't represent 0.10 exactly, so sums drift by fractions of a cent. Smithy has `bigDecimal`, but in JSON it's a bare number, and JavaScript parses every JSON number as a 64-bit float: a partner's Node.js service would round it before your code ever sees it.

So model an amount as a structure, and pick one of two encodings:

`model/money.smithy`:

```smithy
$version: "2.1"

namespace com.example.money

/// An amount of money in minor units: 1050 with EUR is 10.50 EUR, 1050 with JPY is 1050 JPY.
/// The number of decimals per currency comes from ISO 4217.
structure Money {
    @required
    amountMinor: Long

    @required
    currency: CurrencyCode
}

/// An ISO 4217 currency code, such as EUR or USD.
@pattern("^[A-Z]{3}$")
string CurrencyCode

/// An exact decimal written as a string, for rates and fees with more decimals than the currency.
@pattern("^-?[0-9]+(\\.[0-9]+)?$")
string DecimalString
```

- **Minor units in a `Long`** (`amountMinor: 1050` with `EUR` is 10.50 EUR). This is the usual choice for payment amounts: integers add exactly, and every language has them. Two caveats:
  - the number of decimals depends on the currency (JPY has none, BHD has three), so it comes from ISO 4217, not from a fixed "divide by 100";
  - JavaScript loses integer precision above 2^53 (about 90 trillion EUR in cents), which is fine for payments but worth knowing for aggregated ledger totals.
- **A decimal string** (`DecimalString` above) for values with more decimals than the currency allows, such as FX rates, interest rates or per-unit fees. Strings survive every JSON parser unchanged.

Then make the choice stick, with a validator that fails the build on any `float` or `double` member (`NoFloatingPoint` in [the validators](#the-validators)).

Two related traps:
- **Timestamps.** Use `timestamp` with `@timestampFormat("date-time")` for instants (`createdAt`). restJson1 writes timestamps in bodies as epoch seconds by default, which is easy to misread.
- **Business dates.** A value date or a statement date is a calendar day, not an instant: 2026-11-28 shouldn't shift with a time zone. Smithy has no date type, so model it as a `string` with `@pattern("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")`.

## 2. Idempotency and retries

Networks fail after the server has acted but before the client hears back. The client then retries, and without protection the customer pays twice. The standard fix is an idempotency key: the client sends a unique key with each logical request, and the server stores the first result under that key and returns it for every retry.

Smithy models the whole thing:

```smithy
@idempotencyToken
@httpHeader("Idempotency-Key")
idempotencyKey: String
```

- **`@idempotencyToken`** tells every generated client to fill the member with a fresh UUID when the caller leaves it empty, and to keep it across the client's automatic retries. Callers get safe retries without thinking about it. This repo's runtime does this ([ShapeCodec.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/ShapeCodec.scala)).
- **`@httpHeader("Idempotency-Key")`** puts it where payment APIs conventionally expect it.
- **`@readonly`** (GET-like) and **`@idempotent`** (PUT- or DELETE-like) operations are safe to retry by definition, so they don't need a token.
- **`IdempotencyConflict`** (409) is the error for a reused key with a different request body. Model it, so clients can handle it.

The server half is your code: store `(key, request hash) → response` with a retention period, atomically with the payment itself. The validator `WritesNeedIdempotencyKey` makes sure no state-changing, audited operation ships without the token.

## 3. Errors are part of the contract

Partners write code against your errors, so make them shapes, not strings:

```smithy
/// The debtor's account doesn't hold enough to cover the payment.
@error("client")
@httpError(422)
structure InsufficientFunds {
    @required
    message: String

    @required
    available: Money
}

@error("client")
@retryable(throttling: true)
@httpError(429)
structure Throttled {
    message: String
}
```

- **`@error("client")` or `@error("server")`** says whose fault it is. Generated clients raise a typed exception for each, such as `InsufficientFunds` with its `available` balance.
- **`@retryable`** tells generated clients which errors to retry; `throttling: true` makes them back off harder. Business errors (insufficient funds, frozen account) must never be retryable.
- **Operation `errors`** list what each call can fail with, and service-level `errors` (`Throttled`, `ServiceUnavailable`) apply to every call. Clients get an exhaustive list, and the OpenAPI output documents each status code.

## 4. Compliance as traits, checked by the build

Smithy lets you define your own traits. A fintech can use them to put its regulatory knowledge into the model, where tools can see it:

`model/compliance.smithy`:

```smithy
$version: "2.1"

namespace com.example.compliance

/// Personal data under GDPR. The target shape must also be @sensitive.
@trait(selector: "member")
structure pii {}

/// PCI DSS cardholder data. Allowed only in the vault namespace.
@trait(selector: "member")
structure pciData {
    @required
    category: PciCategory
}

enum PciCategory {
    PAN
    CARDHOLDER_NAME
    EXPIRY
    CVV
}

/// OAuth 2.0 scopes a caller needs to invoke the operation.
@trait(selector: "operation")
list requiredScopes {
    member: String
}

/// Every call to this operation is written to the audit log.
@trait(selector: "operation")
structure audited {}
```

Traits on their own are labels. They become rules when a validator checks them. Smithy's `EmitEachSelector` validator emits an event for every shape a selector matches, and an event with severity `DANGER` fails the build:

### The validators

`model/validators.smithy`:

```smithy
$version: "2.1"

// Organisation-wide rules, checked on every build. A violation fails the build.
metadata validators = [
    {
        name: "EmitEachSelector"
        id: "NoFloatingPoint"
        severity: "DANGER"
        message: "float and double lose cents; use com.example.money#Money or a decimal string"
        configuration: {
            selector: "member :test(> :is(float, double))"
        }
    }
    {
        name: "EmitEachSelector"
        id: "PiiMustBeSensitive"
        severity: "DANGER"
        message: "A @pii member must target a @sensitive shape, so it is redacted from logs"
        configuration: {
            selector: "member [trait|com.example.compliance#pii] :not(:test(> [trait|sensitive]))"
        }
    }
    {
        name: "EmitEachSelector"
        id: "PciOnlyInVault"
        severity: "DANGER"
        message: "Cardholder data may only be modelled in the PCI-scoped vault service"
        configuration: {
            selector: "member [trait|com.example.compliance#pciData] :not([id|namespace = 'com.example.vault'])"
        }
    }
    {
        name: "EmitEachSelector"
        id: "WritesNeedIdempotencyKey"
        severity: "DANGER"
        message: "Operations that move money must take an @idempotencyToken"
        configuration: {
            selector: "operation [trait|com.example.compliance#audited] :not([trait|readonly]) :not([trait|idempotent]) :not(:test(-[input]-> structure > member [trait|idempotencyToken]))"
        }
    }
    {
        name: "EmitEachSelector"
        id: "WritesMustBeAudited"
        severity: "DANGER"
        message: "Every operation that changes state must be @audited"
        configuration: {
            selector: "operation :not([trait|readonly]) :not([trait|com.example.compliance#audited])"
        }
    }
]
```

| Rule | What it prevents |
|---|---|
| `NoFloatingPoint` | A `float` or `double` anywhere: amounts that drift by fractions of a cent |
| `PiiMustBeSensitive` | Personal data that isn't `@sensitive`, so it would be printed by loggers and `toString` |
| `PciOnlyInVault` | Card numbers outside the PCI-scoped service, which would pull that service into PCI DSS audit scope |
| `WritesNeedIdempotencyKey` | An audited write without an idempotency token: retries could pay twice |
| `WritesMustBeAudited` | A state-changing operation nobody marked `@audited`, so it would be missing from the audit log |

To check they work, I ran them against a model that breaks each one. Each rule fired, and the build failed:

```text
DANGER NoFloatingPoint com.example.bad#Fee$rate: float and double lose cents; use com.example.money#Money or a decimal string
DANGER PiiMustBeSensitive com.example.bad#Fee$email: A @pii member must target a @sensitive shape, so it is redacted from logs
DANGER PciOnlyInVault com.example.bad#Fee$pan: Cardholder data may only be modelled in the PCI-scoped vault service
DANGER WritesNeedIdempotencyKey com.example.bad#Transfer: Operations that move money must take an @idempotencyToken
DANGER WritesMustBeAudited com.example.bad#CloseAccount: Every operation that changes state must be @audited
```

Card data then lives in its own namespace, the only place the rules allow it:

`model/vault.smithy`:

```smithy
$version: "2.1"

namespace com.example.vault

use com.example.compliance#pciData

/// Card data lives only here, in the PCI-scoped vault. Other services see a token.
structure CardDetails {
    @required
    @pciData(category: "PAN")
    pan: CardNumber

    @required
    @pciData(category: "EXPIRY")
    expiry: CardExpiry
}

@sensitive
@pattern("^[0-9]{12,19}$")
string CardNumber

@sensitive
@pattern("^(0[1-9]|1[0-2])/[0-9]{2}$")
string CardExpiry
```

Other services refer to a card by a token the vault issues, so they stay out of PCI scope.

### A data map for free

GDPR asks you to know where personal data is. With `@pii` on every such member, the model answers that question with one command:

```sh
smithy select --selector 'member [trait|com.example.compliance#pii]'
```

On the payments model it prints `com.example.payments#Creditor$iban` and `com.example.payments#Creditor$name`; on a real model it lists every PII member of every service. The list is always current, because it's computed from the same model the code is generated from.

### Enforcing the traits at run time

Validators check the model. At run time, servers and clients read the same traits from the generated schemas:
- **`@requiredScopes`:** an auth middleware reads the operation's scopes and checks them against the caller's OAuth token. Nobody has to keep a separate permissions table in sync.
- **`@audited`:** a middleware writes an audit record for every call to an operation with the trait.
- **`@sensitive`:** loggers and `toString` replace the value with `*** Sensitive Data Redacted ***`, as the AWS SDKs do.

smithy4s exposes traits as *hints*, smithy-java as schema traits, and this repo's runtime as `Schema.traits`, so the middleware is written once and works for every operation.

## 5. The payments service

The full service, with resources, pagination and a waiter:

`model/payments.smithy`:

```smithy
$version: "2.1"

namespace com.example.payments

use aws.protocols#restJson1
use com.example.compliance#audited
use com.example.compliance#pii
use com.example.compliance#requiredScopes
use com.example.money#Money
use smithy.waiters#waitable

/// Create, read, list and refund payments.
@restJson1
@httpBearerAuth
@title("Payments API")
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "pageSize")
service Payments {
    version: "2026-11-28"
    resources: [Payment]
    errors: [Throttled, ServiceUnavailable]
}

resource Payment {
    identifiers: { paymentId: PaymentId }
    create: CreatePayment
    read: GetPayment
    list: ListPayments
    operations: [RefundPayment]
}

@audited
@requiredScopes(["payments:write"])
@http(method: "POST", uri: "/payments", code: 201)
operation CreatePayment {
    input := {
        /// Clients fill this in when it's missing, and reuse it on retries: the server
        /// returns the first result instead of paying twice.
        @idempotencyToken
        @httpHeader("Idempotency-Key")
        idempotencyKey: String

        @required
        amount: Money

        @required
        debtorAccount: AccountId

        @required
        creditor: Creditor

        reference: PaymentReference
    }

    output := with [PaymentFields] {}

    errors: [InsufficientFunds, AccountFrozen, IdempotencyConflict]
}

@readonly
@requiredScopes(["payments:read"])
@http(method: "GET", uri: "/payments/{paymentId}")
@waitable(
    PaymentSettled: {
        documentation: "Wait until the payment has settled."
        minDelay: 2
        acceptors: [
            {
                state: "success"
                matcher: { output: { path: "status", expected: "SETTLED", comparator: "stringEquals" } }
            }
            {
                state: "failure"
                matcher: { output: { path: "status", expected: "FAILED", comparator: "stringEquals" } }
            }
        ]
    }
)
operation GetPayment {
    input := {
        @required
        @httpLabel
        paymentId: PaymentId
    }

    output := with [PaymentFields] {}

    errors: [PaymentNotFound]
}

@readonly
@requiredScopes(["payments:read"])
@paginated(items: "payments")
@http(method: "GET", uri: "/payments")
operation ListPayments {
    input := {
        @httpQuery("status")
        status: PaymentStatus

        @httpQuery("nextToken")
        nextToken: String

        @httpQuery("pageSize")
        @range(min: 1, max: 100)
        pageSize: Integer
    }

    output := {
        @required
        payments: [PaymentSummary]

        nextToken: String
    }
}

@audited
@requiredScopes(["payments:refund"])
@http(method: "POST", uri: "/payments/{paymentId}/refunds", code: 201)
operation RefundPayment {
    input := {
        @required
        @httpLabel
        paymentId: PaymentId

        @idempotencyToken
        @httpHeader("Idempotency-Key")
        idempotencyKey: String

        @required
        amount: Money
    }

    output := {
        @required
        refundId: String

        @required
        status: PaymentStatus
    }

    errors: [PaymentNotFound, RefundExceedsPayment]
}

@mixin
structure PaymentFields {
    @required
    paymentId: PaymentId

    @required
    status: PaymentStatus

    @required
    amount: Money

    @required
    @timestampFormat("date-time")
    createdAt: Timestamp
}

structure PaymentSummary with [PaymentFields] {}

structure Creditor {
    @required
    @pii
    name: PersonName

    @required
    @pii
    iban: Iban
}

@pattern("^pay_[A-Za-z0-9]{20}$")
string PaymentId

@pattern("^acc_[A-Za-z0-9]{20}$")
string AccountId

@sensitive
@length(min: 1, max: 140)
string PersonName

/// An IBAN: country code, two check digits, then up to 30 letters or digits.
@sensitive
@pattern("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")
string Iban

@length(max: 35)
string PaymentReference

enum PaymentStatus {
    PENDING
    SETTLED
    FAILED
    REFUNDED
}

/// The debtor's account doesn't hold enough to cover the payment.
@error("client")
@httpError(422)
structure InsufficientFunds {
    @required
    message: String

    @required
    available: Money
}

@error("client")
@httpError(423)
structure AccountFrozen {
    @required
    message: String
}

/// The Idempotency-Key was used before, with a different request.
@error("client")
@httpError(409)
structure IdempotencyConflict {
    @required
    message: String
}

@error("client")
@httpError(404)
structure PaymentNotFound {
    @required
    message: String
}

@error("client")
@httpError(422)
structure RefundExceedsPayment {
    @required
    message: String
}

@error("client")
@retryable(throttling: true)
@httpError(429)
structure Throttled {
    message: String
}

@error("server")
@retryable
@httpError(503)
structure ServiceUnavailable {
    message: String
}
```

Points worth noting:
- **Resources** (`resource Payment`) give the API a consistent shape: `create`, `read`, `list` and instance operations bound to `paymentId`. Smithy checks that the identifiers line up.
- **`@paginated`** on the service sets the token names once; `ListPayments` adds `items`. Generated clients get a paginator (this repo's does: `listPaymentsPaginator`).
- **`@waitable`** describes how to wait for a payment to settle, so generated clients get a waiter (named like `waitUntilPaymentSettled`, depending on the generator) instead of every caller writing its own polling loop.
- **Mixins** (`PaymentFields`) keep the create, read and summary shapes identical without copy and paste.
- **`[PaymentSummary]`** is an IDL 2.1 inline list ([inline collections](scala-client.md)).
- **Constraint traits** (`@pattern`, `@length`, `@range`) document the rules and let servers validate input before your code runs. smithy-rs servers, for example, reject invalid input automatically (check what your server generator does). Clients don't validate, by design, so an old client never refuses a value a newer server allows.

## 6. Compatibility: `smithy diff` in CI

Partners integrate once and expect it to keep working. `smithy diff` compares two versions of the model and reports breaking changes. In CI, run it against `main`:

```sh
smithy diff --mode git --old origin/main
```

To test it, I made three changes to the payments model: removed the `REFUNDED` status, lowered `PaymentReference`'s maximum length from 35 to 18, and made `reference` `@required`. Two were reported as errors:

```text
ERROR ChangedEnumTrait.Removed.3 com.example.payments#PaymentStatus: Enum value `REFUNDED` was removed
ERROR ChangedLengthTrait com.example.payments#PaymentReference: smithy.api#length trait value `max` was made more restrictive by lowering from 35 to 18
ERROR RemovedShape com.example.payments#PaymentStatus$REFUNDED: Removed member `com.example.payments#PaymentStatus$REFUNDED`
```

**The third change was not reported, and that's a trap.** Smithy treats members of operation input (`input := { … }` is an `@input` structure) as optional for clients, whatever `@required` says, so making one required doesn't break a generated client's code. But a server that enforces `@required` now rejects every old client that doesn't send `reference`: a production incident that the diff called safe. For a fintech, add a house rule: a new required input member needs a `@default`, or a new operation version. Write it as a custom diff evaluator (`smithy-diff`'s `DiffEvaluator` interface), or as a review checklist item.

Other changes to watch:
- **Adding an enum value** is compatible for Smithy, and safe for clients that handle unknown values. This repo's generated enums have an `UnknownValue(raw)` case for exactly that. Partners' hand-written code might not, so announce new values.
- **Adding a new error** to an operation is allowed, but a partner's exhaustive `match` won't cover it. Document that new errors can appear.

## 7. One model, several protocols

The protocol is a trait on the service, so the same shapes can be served in different ways ([Protocols are not wire formats](http-and-formats.md#protocols-are-not-wire-formats)):

| Who calls | Protocol | Why |
|---|---|---|
| Partners, the web, mobile apps | `restJson1` | Resource URLs, HTTP methods and status codes ([REST in practice](http-and-formats.md#rest-api-in-practice)); converts to OpenAPI for the partner portal and for API Gateway |
| Your own services | `rpcv2Cbor` | Binary CBOR: smaller and faster to parse, with exact `bigDecimal` and binary blobs |
| Events (Kafka, webhooks) | No protocol: the shapes alone | Export a [JSON Schema](json-schema.md) or an [Avro schema](avro-schema.md) for the schema registry, with this repo's `EventSchemas` |

The payments service above converts to OpenAPI with the `openapi` plugin of `smithy-build.json`, with paths `/payments`, `/payments/{paymentId}` and `/payments/{paymentId}/refunds`:

```json
{
    "version": "1.0",
    "sources": ["model"],
    "maven": {
        "dependencies": [
            "software.amazon.smithy:smithy-aws-traits:1.75.1",
            "software.amazon.smithy:smithy-waiters:1.75.1",
            "software.amazon.smithy:smithy-openapi:1.75.1"
        ]
    },
    "plugins": {
        "openapi": {
            "service": "com.example.payments#Payments",
            "protocol": "aws.protocols#restJson1"
        }
    }
}
```

**Webhooks** have no protocol trait in Smithy. Model each webhook payload as a structure in the same repo, so it gets the same validators and diff checks, and publish its JSON Schema. Sign deliveries with HMAC and a timestamp header, and document that in the payload's `@documentation`.

## 8. Generating servers and clients

| Language | Generator | Server | Client |
|---|---|---|---|
| Scala | [smithy4s](https://disneystreaming.github.io/smithy4s/) | Yes (http4s) | Yes |
| Java | [smithy-java](https://github.com/smithy-lang/smithy-java) | Yes | Yes |
| Rust | [smithy-rs](https://github.com/smithy-lang/smithy-rs) | Yes; the server SDK runs AWS services | Yes; the AWS SDK for Rust |
| TypeScript | [smithy-typescript](https://github.com/smithy-lang/smithy-typescript) | Yes (server SDK) | Yes; the AWS SDK for JavaScript v3 |
| Kotlin, Go, Python, Swift | smithy-kotlin, smithy-go, smithy-python, smithy-swift | No | Yes |

A typical setup: servers in one language (Scala with smithy4s, or Rust, or Java), generated clients for your own services, and generated SDKs plus the OpenAPI document for partners. This repo already has the smithy4s sbt plugin in [project/plugins.sbt](../../project/plugins.sbt). Check each generator's protocol support before choosing: not every generator implements every protocol.

## What this repo can do with it today

I ran this repo's generator on the payments model, with `@restJson1` swapped for `@rpcv2Json` because the generator doesn't implement restJson1 yet ([One call through each protocol](protocol-flows.md)). It generated 21 files for `example.payments`, including:
- `PaymentsClient` with `createPayment`, `getPayment`, `listPayments`, `refundPayment` and `listPaymentsPaginator`;
- typed errors (`InsufficientFunds`, `IdempotencyConflict`, …);
- an `idempotencyKey: Option[String]` that the runtime fills in when it's empty;
- `PaymentStatus` with an `UnknownValue(raw)` case, for statuses added later.

What a fintech would need first:

| Gap | Why it matters here | Fix |
|---|---|---|
| `@sensitive` isn't kept | The generated `Creditor(name, iban)` is a plain case class, so its `toString` prints the IBAN, and the wire log shows it too | Add `sensitive` to the generator's kept traits; generate a redacting `toString`; have `WireLog` mask sensitive members |
| No restJson1 | Partner-facing APIs are usually REST | Implement `@http` bindings (labels, query, headers, status codes) |
| No waiters | `@waitable` is ignored, so there's no `waitUntilPaymentSettled` | Generate waiters from the acceptors |
| Mixins generate a type | `PaymentFields` becomes its own case class, which nothing uses | Skip shapes with `@mixin` |
| Client only | No server stubs | Use smithy4s or smithy-java for servers, or add `ScalaServerCodegenPlugin` (a stub today) |

## A rollout plan

1. **Start the model repo** with the compliance traits, the money shapes and the validators, before the first service. Rules are cheap to add on day one and expensive to retrofit.
2. **Model the first service**, convert it to OpenAPI, and review it with a partner before writing any server code.
3. **Wire up CI:** `smithy build` (validators) and `smithy diff --mode git --old origin/main` on every pull request, plus a review checklist for what the diff can't see (new required inputs, new errors, new enum values).
4. **Generate the server stubs** and write only the business logic. Put scope checks, audit logging and redaction in middleware driven by the traits.
5. **Publish SDKs and docs** from the same build, so a release of the model is a release of everything.
6. **Inventory PII** with `smithy select` for GDPR records, and keep the PCI vault in its own namespace and deployment.

## Pitfalls

- **Floats for money.** The `NoFloatingPoint` validator exists because this mistake keeps happening.
- **`bigDecimal` in JSON.** Exact in the model, rounded by any JavaScript client. Use minor units or a decimal string.
- **Trusting `smithy diff` alone.** It calls a new `@required` input member compatible ([above](#6-compatibility-smithy-diff-in-ci)).
- **Closed enums in clients.** A client that crashes on an unknown status turns your next release into its outage. Generate an `UnknownValue` case, as this repo does.
- **`@sensitive` on members.** It's not allowed: Smithy rejects it with `Trait 'sensitive' cannot be applied to …$iban`. Put it on the target shape (`@sensitive string Iban`), as in the model above, which also means every use of `Iban` is covered.
- **Retrying business errors.** Only throttling and server errors should be `@retryable`. Retrying `InsufficientFunds` just repeats the refusal, and retrying a timeout without an idempotency key can pay twice.

## Further reading

- [Smithy: selectors](https://smithy.io/2.0/spec/selectors.html) and [model validation](https://smithy.io/2.0/spec/model-validation.html), for writing more `EmitEachSelector` rules
- [Smithy: behavior traits](https://smithy.io/2.0/spec/behavior-traits.html): `idempotencyToken`, `idempotent`, `readonly`, `retryable`, `paginated`
- [Smithy: waiters](https://smithy.io/2.0/additional-specs/waiters.html)
- [smithy-diff](https://smithy.io/2.0/guides/smithy-diff.html)
- [Converting Smithy to OpenAPI](https://smithy.io/2.0/guides/model-translations/converting-to-openapi.html)
- In this repo: [HTTP and formats](http-and-formats.md), [Schemas in Scala](schema.md), [A Scala client generator](scala-client.md)
