package smithy.scala.codegen

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}

import scala.jdk.CollectionConverters.*

import org.apache.avro.SchemaFormatter

import software.amazon.smithy.jsonschema.{JsonSchemaConfig, JsonSchemaConverter, JsonSchemaMapper, Schema}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.{Shape, ShapeId as SmithyId}
import software.amazon.smithy.model.traits.{SparseTrait, TimestampFormatTrait}
import software.amazon.smithy.model.transform.ModelTransformer

import smithy.scala.runtime.codecs.{AvroSchemas, JsonSettings}

/**
  * Writes a JSON Schema and an Avro schema for Smithy shapes, for event payloads that go to Kafka, a
  * schema registry or a webhook consumer. See docs/smithy/json-schema.md and docs/smithy/avro-schema.md.
  *
  * {{{
  * sbt --client "clientCodegen/runMain smithy.scala.codegen.EventSchemas <model> <output dir> [--json=awsJson] <shape id>..."
  * }}}
  *
  * For each shape it writes `<Name>.schema.json` and `<Name>.avsc`. Both describe the bytes this repo's
  * codecs write:
  *   - the JSON Schema comes from Smithy's own converter (`smithy-jsonschema`), set up to match
  *     `JsonCodec` with the given [[JsonSettings]]: timestamps as epoch seconds unless the settings honour
  *     `@timestampFormat`, `@jsonName` only if they honour it, integers as `"integer"`, NaN and the
  *     infinities as strings, and nulls allowed in `@sparse` lists and maps (the converter itself
  *     ignores `@sparse`);
  *   - the Avro schema is the runtime's own [[AvroSchemas.of]], applied to a schema built from the
  *     model ([[ModelSchemas]]), so it's the schema `AvroSchemaCodec` writes with.
  */
object EventSchemas {

  /** How rpcv2Json writes JSON: no `@jsonName`, every timestamp as epoch seconds. */
  val rpcv2Json: JsonSettings = JsonSettings(useJsonName = false, useTimestampFormat = false)

  /** How awsJson1_0 and awsJson1_1 write JSON: no `@jsonName`, `@timestampFormat` honoured. */
  val awsJson: JsonSettings = JsonSettings(useJsonName = false, useTimestampFormat = true)

  /** A JSON Schema (draft-07) for one shape, with the shapes it uses under `definitions`. */
  def jsonSchema(model: Model, id: SmithyId, settings: JsonSettings = rpcv2Json): String = {
    // A protocol that ignores @timestampFormat writes epoch seconds everywhere, so the schema must too.
    val prepared =
      if (settings.useTimestampFormat) model
      else ModelTransformer.create().removeTraitsIf(model, (_, t) => t.isInstanceOf[TimestampFormatTrait])
    val config = new JsonSchemaConfig()
    config.setDefaultTimestampFormat(TimestampFormatTrait.Format.EPOCH_SECONDS)
    config.setUseJsonName(settings.useJsonName)
    config.setUseIntegerType(true)          // byte, short, integer and long as "integer", not "number"
    config.setSupportNonNumericFloats(true) // JsonCodec writes NaN and the infinities as strings
    // Only the shapes this one uses: a model can hold unrelated shapes with the same name
    // (DynamoDB's Condition and smithy.rules#Condition), which would clash under definitions.
    // The converter checks for clashes across the whole model it's given, so it gets just those.
    val used     = Model.builder().addShapes(new Walker(prepared).walkShapes(prepared.expectShape(id))).build()
    val document = JsonSchemaConverter.builder()
      .model(used)
      .config(config)
      .rootShape(id)
      .addMapper(SparseNulls)
      .build()
      .convert()
    val withDialect = Node.objectNode()
      .withMember("$schema", Node.from("http://json-schema.org/draft-07/schema#"))
      .merge(document.toNode.expectObjectNode)
    Node.prettyPrintJson(withDialect)
  }

  /** Lets a `@sparse` list or map hold nulls, as JsonCodec writes them. */
  private object SparseNulls extends JsonSchemaMapper {
    private val nullSchema = Schema.builder().`type`("null").build()
    private def orNull(s: Schema): Schema = Schema.builder().anyOf(List(s, nullSchema).asJava).build()

    override def updateSchema(shape: Shape, builder: Schema.Builder, config: JsonSchemaConfig): Schema.Builder =
      if (!shape.hasTrait(classOf[SparseTrait])) builder
      else {
        val current = builder.build()
        current.getItems.ifPresent(items => builder.items(orNull(items)): Unit)
        current.getAdditionalProperties.ifPresent(values => builder.additionalProperties(orNull(values)): Unit)
        builder
      }
  }

  /** The Avro schema (`.avsc`) for one shape. */
  def avroSchema(model: Model, id: SmithyId): String =
    SchemaFormatter.format("json/pretty", AvroSchemas.of(ModelSchemas.of(model, id)))

  /** Writes both schemas for each shape, and returns the files written. */
  def write(model: Model, outDir: Path, ids: Seq[SmithyId], settings: JsonSettings = rpcv2Json): Seq[Path] = {
    Files.createDirectories(outDir)
    ids.flatMap { id =>
      val json = outDir.resolve(s"${id.getName}.schema.json")
      val avsc = outDir.resolve(s"${id.getName}.avsc")
      Files.writeString(json, jsonSchema(model, id, settings) + "\n", UTF_8)
      Files.writeString(avsc, avroSchema(model, id) + "\n", UTF_8)
      Seq(json, avsc)
    }
  }

  def main(args: Array[String]): Unit = args.toList match {
    case modelPath :: outDir :: rest if rest.exists(!_.startsWith("--")) =>
      val settings = if (rest.contains("--json=awsJson")) awsJson else rpcv2Json
      val ids      = rest.filterNot(_.startsWith("--")).map(SmithyId.from)
      val written  = write(ModelLoader.load(Paths.get(modelPath)), Paths.get(outDir), ids, settings)
      written.foreach(p => println(s"wrote $p"))
    case _ =>
      System.err.println("usage: EventSchemas <model file or directory> <output dir> [--json=awsJson] <shape id>...")
      sys.exit(2)
  }
}
