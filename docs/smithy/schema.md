# Schemas in Scala: Tapir, ZIO Schema, ZIO Blocks and smithy4s

All four libraries have a type called `Schema`, and so does this playground's runtime. Each library uses it for a different job. This page compares them from their source code, shows what each one is good for in production, and shows how each fits next to a Smithy model.

Read from the published sources jars, as of 2026-11-27:

| Library | Version | Artifact |
|---|---|---|
| [Tapir](https://github.com/softwaremill/tapir) | 1.14.2 | `com.softwaremill.sttp.tapir::tapir-core` |
| [ZIO Schema](https://github.com/zio/zio-schema) | 1.9.2 | `dev.zio::zio-schema`, `-derivation`, `-json`, `-protobuf`, … |
| [ZIO Blocks](https://github.com/zio/zio-blocks) | 0.0.56 | `dev.zio::zio-blocks-schema` and its format modules |
| [smithy4s](https://github.com/disneystreaming/smithy4s) | 0.19.11 (in this build's `project/plugins.sbt`) | `com.disneystreaming.smithy4s::smithy4s-core` |

## What a schema is

A schema is a value that describes a type. It's data you can inspect at runtime, rather than code that only the compiler sees. What a library puts in that value decides what it can do with it:

| What the schema holds | Lets you | Who has it |
|---|---|---|
| **Shape**: fields, cases, element types, names | Generate docs (OpenAPI, JSON Schema), walk a value generically | All of them |
| **Metadata**: descriptions, renames, defaults, formats, validators, traits | Field renames on the wire, validation, docs | All of them |
| **Bindings**: how to build a value from its parts and take it apart | Derive codecs, convert to and from a generic value, diff and patch, migrate between versions | ZIO Schema, ZIO Blocks, smithy4s |

The third row is the dividing line:
- **Description-only schemas** (Tapir, and this repo's runtime `Schema`) can describe a value but can't build one. Encoding and decoding come from somewhere else: circe or jsoniter-scala for Tapir, and the generated shape codecs here.
- **Schemas with bindings** (ZIO Schema, ZIO Blocks, smithy4s) can build and take apart values themselves. One schema can then drive every codec, plus generic tools like diffs and migrations.

## At a glance

| | Tapir | ZIO Schema | ZIO Blocks | smithy4s |
|---|---|---|---|---|
| Type | `Schema[T]` case class | `Schema[A]` sealed trait (an ADT) | `Schema[A](reflect: Reflect.Bound[A])` | `Schema[A]` sealed trait (an ADT) |
| Where it comes from | `Schema.derived[T]`, or `import sttp.tapir.generic.auto.*` | `DeriveSchema.gen[A]` | `Schema.derived` / `derives Schema` | Generated from a Smithy model |
| Holds bindings | No | Yes (`construct` / `deconstruct` on records and enums) | Yes, separately from the structure (`Reflect[F, A]`: `Binding` or `NoBinding`) | Yes (`make` and field getters) |
| Main job | Endpoint docs and validation | Codecs and generic programming | Codecs, optics, migrations | Codecs and protocols for Smithy services |
| Formats | None; the JSON library does the encoding | JSON, Protobuf, Avro, Thrift, BSON, MessagePack, XML | JSON (built in), Avro, MessagePack, BSON, Thrift, TOON, CSV, XML, YAML | JSON (`smithy4s-json`), XML (`smithy4s-xml`), Protobuf (`smithy4s-protobuf`), through `SchemaVisitor` compilers |
| JSON Schema / OpenAPI | Yes (tapir-openapi-docs) | Through `zio-http` endpoints | `schema.toJsonSchema`, and `Schema.fromJsonSchema` the other way | OpenAPI comes from the Smithy model (smithy-openapi) |
| Validation | `Validator[T]`, run on decode | `Validation[A]` annotations | `Validation` on primitives | Smithy constraint traits (`@length`, `@range`, `@pattern`) |
| Generic value | No | `DynamicValue` | `DynamicValue` | `Document` |
| Diff and patch | No | `schema.diff`, `schema.patch` | `schema.diff`, `schema.patch` | No |
| Migrations | No | `schema.migrate(newSchema)` (structural) | Typed `MigrationBuilder`, checked at compile time | Model evolution rules (smithy-diff) |
| Optics | No | `makeAccessors` (zio-schema-optics) | `Lens`, `Prism`, `Optional`, `Traversal` from `optic(_.field)` | No |

## Tapir: a schema for documentation

Tapir describes HTTP endpoints as values and interprets them as servers, clients or OpenAPI docs. Its `Schema` ([Schema.scala](https://github.com/softwaremill/tapir/blob/master/core/src/main/scala/sttp/tapir/Schema.scala)) describes how a body looks once encoded:

```scala
case class Schema[T](
    schemaType: SchemaType[T],        // SString, SInteger, SNumber, SBoolean, SOption, SArray, SBinary,
                                      // SDate, SDateTime, SProduct, SOpenProduct, SCoproduct, SRef
    name: Option[SName] = None,
    isOptional: Boolean = false,
    description: Option[String] = None,
    default: Option[(T, Option[Any])] = None,
    format: Option[String] = None,    // "int32", "int64", "uuid", ...
    encodedExample: Option[Any] = None,
    deprecated: Boolean = false,
    hidden: Boolean = false,
    validator: Validator[T] = Validator.pass[T],
    attributes: AttributeMap = AttributeMap.Empty
)
```

Its own scaladoc says it "describes the type `T`: its low-level representation, meta-data and validation rules". Nothing in it encodes or decodes. A product field (`SProductField`) carries a getter, `T => Option[FieldType]`, so validators and docs can look inside a value, but there's no constructor. The JSON bytes come from a separate JSON library: `jsonBody[T]` from `tapir-json-circe` or `tapir-jsoniter-scala` needs that library's encoder and decoder *and* a Tapir `Schema[T]`.

The two meet in Tapir's `Codec`: its `decode` runs the JSON library's decoder, then `schema.applyValidation`. A body that parses but breaks a `Validator` is rejected with a 400 before your handler runs.

### Using it in production

```scala
import sttp.tapir.*
import sttp.tapir.json.jsoniter.*           // or json.circe.*
import sttp.tapir.Schema.annotations.*
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

final case class Book(
    title: String,
    @description("ISBN-13, digits only") isbn: String,
    year: Int
)
object Book {
  given Schema[Book] = Schema.derived[Book].modify(_.year)(_.validate(Validator.min(1450)))
  given JsonValueCodec[Book] = JsonCodecMaker.make   // jsoniter-scala does the bytes
}

val addBook = endpoint.post.in("books").in(jsonBody[Book]).out(jsonBody[Book])
// OpenAPIDocsInterpreter().toOpenAPI(addBook, "Books", "1.0")  -> OpenAPI with the description and minimum
```

- **Prefer `Schema.derived[T]` in the companion over `import sttp.tapir.generic.auto.*`.** Automatic derivation re-derives at every use site, which costs compile time in large code bases. Tapir's own "Schema not found" message recommends `Schema.derived` for debugging too.
- **Make the schema agree with the JSON codec.** Tapir doesn't check this. If jsoniter renames fields to snake_case, set `given Configuration = Configuration.default.withSnakeCaseMemberNames` for Tapir as well. Otherwise the OpenAPI document describes a wire format you don't send. The same goes for sealed-trait discriminators: `.withDiscriminator("type")` on both sides.
- **Put validation in the schema** (`Validator.min`, `pattern`, `enumeration`, `@validate`). It's enforced on decode and published in the docs, from one definition.
- **Golden-test the OpenAPI output.** Write the generated YAML to a file in a test and diff it in review. That makes API changes visible.

Use Tapir when the product is an HTTP API with accurate docs. Its schema is about the documentation and validation layer, not about bytes.

## ZIO Schema: a reified ADT that drives codecs

In ZIO Schema ([Schema.scala](https://github.com/zio/zio-schema/blob/main/zio-schema/shared/src/main/scala/zio/schema/Schema.scala)), the schema is a sealed trait with one case per kind of type:
- `Primitive(StandardType)` covers 32 standard types, from `UnitType` and `StringType` to `InstantType`, `ZonedDateTimeType`, `UUIDType` and `CurrencyType`.
- `Optional`, `Sequence`, `Set`, `Map`, `NonEmptySequence`, `NonEmptyMap`, `Tuple2`, `Either`, `Fallback`.
- `Record`: `CaseClass0` to `CaseClass22`, plus `GenericRecord` for records without a Scala class.
- `Enum`: `Enum1` to `Enum22`, plus `EnumN`.
- `Transform`, `Lazy` (recursion), `Dynamic`, `Fail`.

Records and enums carry their bindings: a case class schema holds its fields with getters and a `construct` function. That is what lets one schema derive every codec and generic operation:

```scala
sealed trait Schema[A] {
  def annotations: Chunk[Any]
  def toDynamic(value: A): DynamicValue
  def fromDynamic(value: DynamicValue): Either[String, A]
  def diff(thisValue: A, thatValue: A): Patch[A]
  def patch(oldValue: A, diff: Patch[A]): Either[String, A]
  def migrate[B](newSchema: Schema[B]): Either[String, A => Either[String, B]]
  def transform[B](f: A => B, g: B => A): Schema[B]
  def validate(value: A)(implicit schema: Schema[A]): Chunk[ValidationError]
  def ast: MetaSchema               // the schema as data: serialize it, send it, compare it
  def ordering: Ordering[A]
  // ...
}
```

Annotations (`zio.schema.annotation`) change the wire form without changing the type:
- naming: `@fieldName`, `@fieldNameAliases`, `@caseName`, `@caseNameAliases`;
- dropping things: `@transientField`, `@transientCase`;
- defaults and optional fields: `@fieldDefaultValue`, `@optionalField`;
- discriminators: `@discriminatorName`, `@noDiscriminator`;
- strictness: `@rejectExtraFields`, `@simpleEnum`;
- validation: `@validate`;
- Protobuf field numbers: `@fieldNumber(n)` in zio-schema-protobuf.

### Using it in production

```scala
import zio.schema.*
import zio.schema.annotation.*
import zio.schema.codec.{JsonCodec, ProtobufCodec, BinaryCodec}

final case class Book(title: String, @fieldName("isbn13") isbn: String, year: Int)
object Book {
  implicit val schema: Schema[Book] = DeriveSchema.gen[Book]
  val json: BinaryCodec[Book]  = JsonCodec.schemaBasedBinaryCodec[Book]
  val proto: BinaryCodec[Book] = ProtobufCodec.protobufCodec[Book]
}

Book.json.encode(book)                 // Chunk[Byte]
Book.proto.decode(bytes)               // Either[DecodeError, Book]
Book.schema.diff(before, after)        // Patch[Book], e.g. for an audit log
```

- **Build each codec once and keep it in a `val`.** Codecs are built from the schema at runtime, and some build steps aren't free. The JSON codec builds an encoder per schema and caches it in a `ConcurrentHashMap`. The Protobuf codec walks the schema for each value it encodes (`Encoder.process(schema, value)`).
- **Pin Protobuf field numbers with `@fieldNumber`** if anything outside your service reads the bytes. Without it, numbers follow field order, and reordering fields breaks compatibility.
- **Use `DynamicValue` and `schema.ast` at the edges:** event stores, generic admin tools, data sent to services that don't share your classes. `MetaSchema` makes the schema itself serializable, so a reader can decode data it has no class for.
- **Recursive types need `Schema.defer`** (or derivation handles it with `Lazy`).
- **ZIO Schema fits best inside a ZIO stack.** zio-http endpoints, zio-json and zio-streams `ZPipeline` codecs all build on it.

## ZIO Blocks: structure and bindings kept apart

ZIO Blocks ([Schema.scala](https://github.com/zio/zio-blocks/blob/main/schema/shared/src/main/scala/zio/blocks/schema/Schema.scala)) is the newer design from the same community, written for Scala 2.13 and 3. Its schema is a thin wrapper around a `Reflect`:

```scala
final case class Schema[A](reflect: Reflect.Bound[A]) {
  lazy val jsonCodec: JsonCodec[A]
  def derive[D, TC[_]](d: D)(implicit ev: Derivable[D, TC]): TC[A]   // derive(JsonFormat), derive(AvroFormat), ...
  def getInstance[F <: Format](format: F): format.TypeClass[A]       // derived once, cached per format
  def toDynamicValue(value: A): DynamicValue
  def fromDynamicValue(value: DynamicValue): Either[SchemaError, A]
  def diff(oldValue: A, newValue: A): Patch[A]
  def get[B](optic: Optic[A, B]): Option[Reflect.Bound[B]]           // the schema of a nested part
  def toJsonSchema: JsonSchema
  def doc(value: String): Schema[A]
  def examples(value: A, values: A*): Schema[A]
  // ...
}
```

`Reflect[F[_, _], A]` has eight cases: `Record`, `Variant`, `Sequence`, `Map`, `Primitive`, `Wrapper`, `Dynamic` and `Deferred` (for recursion). The type parameter `F` is the key idea:
- `Reflect.Bound[A] = Reflect[Binding, A]` carries constructors and deconstructors.
- The unbound form uses `NoBinding`, with the same structure and no functions.

So the structure can be inspected, printed, serialized (`toDynamicSchema`) or built from JSON Schema (`Schema.fromJsonSchema`) without any functions inside it. A `Constructor` builds a value from a `Registers` buffer (`construct(in: Registers, offset: RegisterOffset): A`), not from an `Array[Any]`.

Codecs come from a `Deriver[TC]`, which has one method per `Reflect` case (`deriveRecord`, `deriveVariant`, `deriveSequence`, `deriveMap`, `derivePrimitive`, `deriveWrapper`, `deriveDynamic`). Each format module supplies a deriver and a `Format` object: `JsonFormat`, `AvroFormat`, `MessagePackFormat`, and so on. `schema.getInstance(format)` derives the codec once and caches it on the schema.

Typed optics come from the same schema. A companion that extends `CompanionOptics[A]` gets `optic(_.path)` and `$(_.path)` macros, giving `Lens`, `Prism`, `Optional` and `Traversal`. Paths can include `.when[Case]`, `.each`, `.at(i)`, `.atKey(k)` and `.eachValue`.

Migrations are typed: `MigrationBuilder[A, B]` has `renameField`, `addField`, `dropField`, `transformField`, `optionalizeField`, `renameCase` and so on, using selector lambdas. `.build` requires a `MigrationComplete[A, B, Changeset]` instance, which a macro provides only once every difference between `A` and `B` is handled. A migration you forgot about is a compile error. `Migration` also has `reverse` and `andThen`.

Modifiers are the annotations:
- for fields: `@Modifier.rename`, `@Modifier.alias`, `@Modifier.transient`, `@Modifier.id`;
- for types: `discriminator`, `noExtraFields`, `fieldNaming`, `caseNaming`.

### Using it in production

```scala
import zio.blocks.schema.*
import zio.blocks.schema.json.JsonFormat
import zio.blocks.schema.avro.AvroFormat

final case class Book(title: String, @Modifier.rename("isbn13") isbn: String, year: Int) derives Schema
object Book extends CompanionOptics[Book] {
  val year = optic(_.year)                         // Lens[Book, Int]
}

val json = Schema[Book].derive(JsonFormat)         // JsonCodec[Book]
json.encode(book)                                  // Array[Byte]
json.decode(bytes)                                 // Either[SchemaError, Book]
val avro = Schema[Book].derive(AvroFormat)
Book.year.replace(book, 2026)
Schema[Book].toJsonSchema                          // publish it, or check payloads against it
```

- **Pin the version.** This is 0.0.x. In this repo it's 0.0.56; `build.sbt` notes that the `0.017` on Maven Central is a stray, mis-numbered release that sbt-updates suggests anyway.
- **Check what each format supports before relying on it.** [Other formats through ZIO Blocks](scala-client.md#other-formats-through-zio-blocks) lists what this repo found for `DynamicValue` codecs in 0.0.56. BSON's isn't implemented, CSV has none, Thrift and XML can't read back, and TOON fails on quoted strings containing `:`.
- **Use typed migrations for stored data.** When a persisted type changes, a `Migration[V1, V2]` that the compiler checks for completeness beats a hand-written converter that silently ignores a new field.
- **`toJsonSchema` and `fromJsonSchema`** let you publish a contract from the type, or accept a contract (as `Schema[Json]`) from someone else.

## smithy4s: a schema generated from a Smithy model

smithy4s is the one where the schema isn't derived from Scala code. Its code generator (the `smithy4s-sbt-codegen` plugin, already in this build) reads a Smithy model and writes Scala types, each with a `Schema` value ([Schema.scala](https://github.com/disneystreaming/smithy4s/blob/series/0.19/modules/core/src/smithy4s/schema/Schema.scala)):

```scala
sealed trait Schema[A] {
  def shapeId: ShapeId
  def hints: Hints                                 // the Smithy traits, typed
  final def compile[F[_]](fk: Schema ~> F): F[A]   // a SchemaVisitor turns the schema into F[A]
}
// PrimitiveSchema, CollectionSchema, MapSchema, EnumerationSchema,
// StructSchema(fields, make), UnionSchema(alternatives, ordinal),
// OptionSchema, BijectionSchema, RefinementSchema, LazySchema
```

Every schema carries its Smithy `ShapeId`. Its `hints` are the model's traits as typed values: `@jsonName`, `@timestampFormat`, `@length`, `@httpLabel` and so on. Codecs are `SchemaVisitor`s, compiled with a `CachedSchemaCompiler` so each schema is compiled once. Protocol implementations (`simpleRestJson`, AWS JSON, XML) read the same hints to put members in HTTP labels, headers or bodies.

### Using it in production

- **The model is the contract.** Change the `.smithy` file, regenerate, and let the compiler show every place affected. Smithy's own tools check that changes are safe (smithy-diff) and produce OpenAPI (smithy-openapi). So there's nothing to keep in sync by hand.
- **Generated code isn't edited.** Customize through traits in the model (`@jsonName`, `@timestampFormat`, `alloy#...`), not in Scala.
- **Schemas without generated code.** `smithy4s-dynamic` builds the same `Schema` values from a model loaded at run time, for gateways, test tools or admin UIs that handle any service.
- **Use it for services with several clients or languages.** The same model feeds smithy4s for Scala and the official generators for other languages (see [The Smithy ecosystem](ecosystem.md)).

## This repo's `Schema`

The runtime in [Schema.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Schema.scala) is closest to Tapir's: a description with no bindings. Its fields are `id`, `shapeType`, `members` and a `Traits` map holding the few traits the runtime needs. It isn't even typed (`Schema`, not `Schema[A]`). The bindings live in the generated shape codecs, written by Hearth or Mirror derivation, which call a `ShapeWriter` with each member's schema. [Schema, shape codec and format codec](scala-client.md#schema-shape-codec-and-format-codec) explains why: the schema stays plain data, and the codecs are compiled code instead of a schema interpreted at runtime.

The ZIO Blocks bridge is where the two meet. Our shape codecs write a ZIO Blocks `DynamicValue`, and ZIO Blocks' format codecs write the bytes. It's also why the bridge costs a tree per message (see [the benchmarks](scala-client.md#speed-jackson-against-zio-blocks)).

## Choosing, for production

| You need | Use | Because |
|---|---|---|
| An HTTP API with accurate OpenAPI docs and request validation | **Tapir**, with jsoniter-scala or circe for the bytes | Its schema exists for documentation and validation, and they're enforced on decode |
| One Scala type, many formats (JSON, Protobuf, Avro, …), inside a ZIO application | **ZIO Schema** | Mature, many codecs, integrated with zio-http and zio-streams |
| Many formats, plus typed optics and compile-time-checked migrations, and you can accept 0.0.x | **ZIO Blocks** | Structure separate from bindings; codecs derived once per format and cached; typed migrations |
| A contract shared across services or languages | **Smithy + smithy4s** (or this repo's generator) | The model is the source of truth; Scala types and schemas are generated from it |
| The fastest and smallest wire format for your own generated types | Compiled codecs over a description-only schema, like this repo's | Hearth-derived codecs with a streaming format codec (`AvroSchemaCodec`) beat every bridge here |

They combine well:
- **Tapir + smithy4s.** Expose a Smithy-modelled service over HTTP with smithy4s's own routing, or put a Tapir schema next to a smithy4s type when an endpoint is written in Tapir. Then make sure both describe the same JSON.
- **Smithy for the contract, ZIO Blocks for storage.** Keep the API in Smithy, persist events with ZIO Blocks schemas, and use `MigrationBuilder` when stored shapes change.
- **ZIO Schema or ZIO Blocks behind Tapir.** Tapir still needs its own `Schema[T]` for docs. Derive it next to the other one so field names and discriminators match.

What all four have in common in production:
- **Derive once.** Put the schema and its codecs in the companion as `given` or `val`. Re-deriving at every use site costs compile time (auto-derivation) or run time (codec construction).
- **One source of truth for names.** Each library has its own renames: Tapir's `Configuration` and `@encodedName`, ZIO Schema's `@fieldName`, ZIO Blocks' `@Modifier.rename`, Smithy's `@jsonName`. When two of them describe the same JSON, write a test that encodes with one and checks the result against the other.
- **Test round trips and compatibility.** Round-trip every format you ship. Keep golden files of encoded messages and generated docs, so a change in the wire format shows up in review.
- **Plan for evolution.** Add fields as optional or with defaults, never reuse Protobuf field numbers, and decide whether unknown fields are rejected or ignored:
  - Tapir: up to the JSON library;
  - ZIO Schema: `@rejectExtraFields`;
  - ZIO Blocks: `Modifier.noExtraFields`;
  - Smithy clients: ignore unknown fields by design.

Back to [the overview](README.md).
