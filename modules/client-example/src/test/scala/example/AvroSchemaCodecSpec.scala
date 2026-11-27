package example

import smithy.scala.runtime.*
import smithy.scala.runtime.codecs.*

import java.io.ByteArrayOutputStream
import java.time.Instant

import scala.jdk.CollectionConverters.*

import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.{Schema as AvroSchema}

import example.dynamodb.*
import example.weather.*

/** Avro for the shape itself: round-trips, the generated Avro schema, and interop with Apache Avro's generic API. */
class AvroSchemaCodecSpec extends munit.FunSuite {
  private val codec = new AvroSchemaCodec

  private def write[A](c: Codec, value: A)(using sc: SchemaCodec[A]): Array[Byte] = c.serialize(w => sc.write(w, sc.schema, value))
  private def read[A](c: Codec, bytes: Array[Byte])(using sc: SchemaCodec[A]): A  = c.deserialize(bytes)(r => sc.read(r, sc.schema))

  private val table = TableDescription(
    tableName = Some("orders"),
    tableStatus = Some(TableStatus.ACTIVE),
    creationDateTime = Some(Instant.parse("2026-11-27T09:30:00.250123Z")),
    itemCount = Some(42L),
    provisionedThroughput = Some(ProvisionedThroughputDescription(readCapacityUnits = Some(5L), writeCapacityUnits = Some(1L)))
  )

  private val put = Put(
    item = Map(
      "id"    -> AttributeValue.S("o-1"),
      "data"  -> AttributeValue.B(Blob.fromUtf8("bytes")),
      "tags"  -> AttributeValue.L(List(AttributeValue.N("1"), AttributeValue.BOOL(true), AttributeValue.NULL(true))),
      "inner" -> AttributeValue.M(Map("deep" -> AttributeValue.M(Map.empty)))
    ),
    tableName = "orders"
  )

  private val forecast = GetForecastOutput(
    days = Some(List(Some(Day(date = Some(Instant.parse("2026-11-28T00:00:00Z")), copy_ = Some("cold"))), None)),
    next = Some("token: with a colon"),
    `type` = Kind.Sunny
  )

  test("structures, enums, longs and microsecond timestamps round-trip") {
    assertEquals(read[TableDescription](codec, write(codec, table)), table)
  }

  test("maps, blobs and recursive unions round-trip") {
    assertEquals(read[Put](codec, write(codec, put)), put)
  }

  test("sparse lists with nulls, Unit union members and unions with a value round-trip") {
    assertEquals(read[GetForecastOutput](codec, write(codec, forecast)), forecast)
    val rain = forecast.copy(`type` = Kind.Rain(3))
    assertEquals(read[GetForecastOutput](codec, write(codec, rain)), rain)
  }

  test("the Avro schema: nullable fields, wrapped unions, logical timestamps, recursion by name") {
    val s = AvroSchemas.of(TableDescription.schema)
    assertEquals(s.getFullName, "com.amazonaws.dynamodb.TableDescription")
    val created = s.getField("CreationDateTime").schema()
    assertEquals(created.getTypes.asScala.map(_.getType).toList, List(AvroSchema.Type.NULL, AvroSchema.Type.LONG))
    assertEquals(created.getTypes.get(1).getLogicalType.getName, "timestamp-micros")

    val av     = AvroSchemas.of(AttributeValue.schema)
    val member = av.getField("member").schema()
    assertEquals(member.getTypes.asScala.map(_.getName).take(3).toList, List("AttributeValue_S", "AttributeValue_N", "AttributeValue_B"))
    // AttributeValue_M.value is a map of AttributeValue: the same record, by name.
    assert(member.getTypes.get(6).getField("value").schema().getValueType eq av)
    // The schema is valid Avro: it survives a print and parse.
    assertEquals(new AvroSchema.Parser().parse(av.toString(true)), av)
  }

  test("Apache Avro's generic reader reads what we write") {
    val schema = AvroSchemas.of(TableDescription.schema)
    val record = new GenericDatumReader[GenericRecord](schema)
      .read(null, DecoderFactory.get().binaryDecoder(write(codec, table), null))
    assertEquals(record.get("TableName").toString, "orders")
    assertEquals(record.get("TableStatus").toString, "ACTIVE")
    assertEquals(record.get("ItemCount"), java.lang.Long.valueOf(42L))
    assertEquals(record.get("CreationDateTime"), java.lang.Long.valueOf(1795771800250123L))
    assertEquals(record.get("ProvisionedThroughput").asInstanceOf[GenericRecord].get("ReadCapacityUnits"), java.lang.Long.valueOf(5L))
    assertEquals(record.get("KeySchema"), null)
  }

  test("we read what Apache Avro's generic writer writes") {
    val schema = AvroSchemas.of(GetForecastOutput.schema)
    val kind   = schema.getField("type").schema().getTypes.get(1) // the nullable union's non-null branch
    val rain   = kind.getField("member").schema().getTypes.asScala.find(_.getName == "Kind_rain").get
    val record = new GenericData.Record(schema)
    record.put("next", "n")
    val rainRecord = new GenericData.Record(rain)
    rainRecord.put("value", 7)
    val kindRecord = new GenericData.Record(kind)
    kindRecord.put("member", rainRecord)
    record.put("type", kindRecord)
    val out = new ByteArrayOutputStream()
    val enc = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[GenericRecord](schema).write(record, enc)
    enc.flush()
    assertEquals(read[GetForecastOutput](codec, out.toByteArray), GetForecastOutput(next = Some("n"), `type` = Kind.Rain(7)))
  }

  test("smaller than the generic DynamicValue Avro and than JSON") {
    val json    = new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = false))
    val sizes   = List(codec, new AvroCodec, json).map(c => write(c, put).length)
    val List(shape, dynamic, jsonSize) = sizes: @unchecked
    assert(shape < dynamic && shape < jsonSize, sizes.toString)
  }
}
