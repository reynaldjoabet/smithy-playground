package example

import smithy.scala.codegen.{EventSchemas, Main}
import smithy.scala.runtime.*
import smithy.scala.runtime.codecs.*

import java.nio.file.{Files, Path, Paths}
import java.time.Instant

import scala.jdk.CollectionConverters.*

import com.networknt.schema.{InputFormat, SchemaRegistry, SpecificationVersion}
import org.apache.avro.Schema as AvroSchema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId as SmithyId
import tools.jackson.databind.json.JsonMapper

import example.dynamodb.*
import example.weather.*

/**
  * The schemas `EventSchemas` exports from a Smithy model describe the bytes our codecs really write:
  * JsonCodec output passes the JSON Schema, and AvroSchemaCodec output is read by Apache Avro's own
  * generic reader with the exported `.avsc`.
  */
class EventSchemasSpec extends munit.FunSuite {

  private val root: Path =
    Iterator.iterate(Paths.get(sys.props("user.dir")).toAbsolutePath)(_.getParent).find(p => Files.exists(p.resolve("build.sbt"))).get

  private lazy val weather: Model  = Main.loadModel(root.resolve("modules/client-example/model/weather.smithy"))
  private lazy val dynamodb: Model = Main.loadModel(root.resolve("src/main/resources/models/dynamodb/dynamodb.json"))

  // Weather is an rpcv2Json service and DynamoDB an awsJson1_0 one: each codec as its protocol sets it up.
  private val rpcJson = new JsonCodec(EventSchemas.rpcv2Json)
  private val awsJson = new JsonCodec(EventSchemas.awsJson)
  private val avro = new AvroSchemaCodec

  private def bytes[A](codec: Codec, value: A)(using c: SchemaCodec[A]): Array[Byte] =
    codec.serialize(w => c.write(w, c.schema, value))

  /** The JSON Schema errors for a document (empty when it's valid). */
  private def jsonErrors(model: Model, id: String, document: String, settings: JsonSettings): List[String] = {
    val schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
      .getSchema(EventSchemas.jsonSchema(model, SmithyId.from(id), settings), InputFormat.JSON)
    schema.validate(JsonMapper.builder().build().readTree(document)).asScala.toList.map(_.toString)
  }

  /** Reads Avro bytes with Apache Avro's generic reader and the exported schema: no code of ours. */
  private def readGeneric(model: Model, id: String, data: Array[Byte]): GenericRecord = {
    val schema = new AvroSchema.Parser().parse(EventSchemas.avroSchema(model, SmithyId.from(id)))
    new GenericDatumReader[GenericRecord](schema).read(null, DecoderFactory.get().binaryDecoder(data, null))
  }

  private val table = TableDescription(
    tableName = Some("orders"),
    tableStatus = Some(TableStatus.ACTIVE),
    creationDateTime = Some(Instant.parse("2026-11-27T09:30:00.250Z")),
    itemCount = Some(42L),
    provisionedThroughput = Some(ProvisionedThroughputDescription(readCapacityUnits = Some(5L), writeCapacityUnits = Some(1L)))
  )

  private val put = Put(
    item = Map(
      "id"   -> AttributeValue.S("o-1"),
      "data" -> AttributeValue.B(Blob.fromUtf8("bytes")),
      "tags" -> AttributeValue.L(List(AttributeValue.N("1"), AttributeValue.M(Map("deep" -> AttributeValue.BOOL(true)))))
    ),
    tableName = "orders"
  )

  private val forecast = GetForecastOutput(
    days = Some(List(Some(Day(date = Some(Instant.parse("2026-11-28T00:00:00Z")), copy_ = Some("cold"))), None)),
    `type` = Kind.Rain(3)
  )

  // ---------------------------------------------------------------------------------------------- Avro

  test("the Avro schema from the model is the one generated code uses") {
    def exported(model: Model, id: String) = new AvroSchema.Parser().parse(EventSchemas.avroSchema(model, SmithyId.from(id)))
    assertEquals(exported(weather, "example.weather#GetForecastOutput"), AvroSchemas.of(GetForecastOutput.schema))
    assertEquals(exported(dynamodb, "com.amazonaws.dynamodb#TableDescription"), AvroSchemas.of(TableDescription.schema))
    assertEquals(exported(dynamodb, "com.amazonaws.dynamodb#Put"), AvroSchemas.of(Put.schema))
  }

  test("Apache Avro reads AvroSchemaCodec output with the exported .avsc") {
    val record = readGeneric(dynamodb, "com.amazonaws.dynamodb#TableDescription", bytes(avro, table))
    assertEquals(record.get("TableName").toString, "orders")
    assertEquals(record.get("TableStatus").toString, "ACTIVE")
    assertEquals(record.get("ItemCount").asInstanceOf[java.lang.Long].longValue, 42L)
    assertEquals(record.get("CreationDateTime").asInstanceOf[java.lang.Long].longValue, Instant.parse("2026-11-27T09:30:00.250Z").toEpochMilli * 1000) // timestamp-micros
  }

  test("Apache Avro reads recursive unions (AttributeValue) and sparse lists") {
    val put2 = readGeneric(dynamodb, "com.amazonaws.dynamodb#Put", bytes(avro, put))
    assertEquals(put2.get("TableName").toString, "orders")
    val f = readGeneric(weather, "example.weather#GetForecastOutput", bytes(avro, forecast))
    assertEquals(f.get("days").asInstanceOf[java.util.List[?]].size, 2)
    assert(f.get("days").asInstanceOf[java.util.List[?]].get(1) == null, "a sparse list keeps its null")
  }

  // ---------------------------------------------------------------------------------------------- JSON Schema

  test("JsonCodec output passes the exported JSON Schema") {
    val cases = List(
      (weather, "example.weather#GetForecastOutput", bytes(rpcJson, forecast), EventSchemas.rpcv2Json),
      (dynamodb, "com.amazonaws.dynamodb#TableDescription", bytes(awsJson, table), EventSchemas.awsJson),
      (dynamodb, "com.amazonaws.dynamodb#Put", bytes(awsJson, put), EventSchemas.awsJson)
    )
    cases.foreach { (model, id, data, settings) =>
      assertEquals(jsonErrors(model, id, new String(data, "UTF-8"), settings), Nil, id)
    }
  }

  test("the JSON Schema rejects a missing required member, a wrong type and an unknown enum value") {
    val id    = "example.weather#GetForecastOutput"
    val table = "com.amazonaws.dynamodb#TableDescription"
    assert(jsonErrors(weather, id, """{"next":"t"}""", EventSchemas.rpcv2Json).nonEmpty, "missing required type")
    assert(jsonErrors(weather, id, """{"type":{"rain":"three"}}""", EventSchemas.rpcv2Json).nonEmpty, "rain is an integer")
    assert(jsonErrors(dynamodb, table, """{"CreationDateTime":"2026-11-27"}""", EventSchemas.awsJson).nonEmpty, "epoch seconds")
    assert(jsonErrors(dynamodb, table, """{"ItemCount":10.5}""", EventSchemas.awsJson).nonEmpty, "a long is an integer")
    // Smithy enums are open, but the converter writes a closed JSON Schema enum: a new value fails.
    assert(jsonErrors(dynamodb, table, """{"TableStatus":"NOT_A_STATUS"}""", EventSchemas.awsJson).nonEmpty)
  }

  test("EventSchemas.write writes a .schema.json and an .avsc per shape") {
    val dir     = Files.createTempDirectory("event-schemas")
    val written = EventSchemas.write(weather, dir, Seq(SmithyId.from("example.weather#GetForecastOutput")))
    assertEquals(written.map(_.getFileName.toString).toList, List("GetForecastOutput.schema.json", "GetForecastOutput.avsc"))
    assert(Files.readString(written.head).contains("\"$schema\""))
  }
}
