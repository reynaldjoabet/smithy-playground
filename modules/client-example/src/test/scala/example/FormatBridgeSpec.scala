package example

import smithyscala.runtime.*
import smithyscala.runtime.codecs.*

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

import zio.blocks.schema.DynamicValue
import zio.blocks.schema.PrimitiveValue

import example.dynamodb.*
import example.weather.*

/**
  * The ZIO Blocks format bridge: generated shapes written through `DynamicValueWriter` and encoded by
  * a ZIO Blocks format, then read back. The shape codecs are the generated (Hearth) ones, unchanged.
  */
class FormatBridgeSpec extends munit.FunSuite {

  private def write[A](codec: Codec, value: A)(using c: SchemaCodec[A]): Array[Byte] =
    codec.serialize(w => c.write(w, c.schema, value))

  private def read[A](codec: Codec, bytes: Array[Byte])(using c: SchemaCodec[A]): A =
    codec.deserialize(bytes)(r => c.read(r, c.schema))

  private val table = TableDescription(
    tableName = Some("orders"),
    tableStatus = Some(TableStatus.ACTIVE),
    creationDateTime = Some(Instant.parse("2026-11-27T09:30:00.250Z")),
    itemCount = Some(42L),
    provisionedThroughput = Some(ProvisionedThroughputDescription(readCapacityUnits = Some(5L), writeCapacityUnits = Some(1L)))
  )

  private val put = Put(
    item = Map(
      "id"    -> AttributeValue.S("o-1"),
      "data"  -> AttributeValue.B(Blob.fromUtf8("bytes")),
      "tags"  -> AttributeValue.L(List(AttributeValue.N("1"), AttributeValue.BOOL(true))),
      "inner" -> AttributeValue.M(Map("deep" -> AttributeValue.M(Map.empty)))
    ),
    tableName = "orders"
  )

  private val forecast = GetForecastOutput(
    days = Some(List(Some(Day(date = Some(Instant.parse("2026-11-28T00:00:00Z")), copy_ = Some("cold"))), None)),
    `type` = Kind.Sunny
  )

  private val roundTripping = List(new ZioBlocksJsonCodec, new MessagePackCodec, new AvroCodec, new YamlCodec)

  roundTripping.foreach { codec =>
    test(s"${codec.mediaType}: structures, enums, timestamps and longs round-trip") {
      assertEquals(read[TableDescription](codec, write(codec, table)), table)
    }
    test(s"${codec.mediaType}: maps, blobs and recursive unions round-trip") {
      assertEquals(read[Put](codec, write(codec, put)), put)
    }
    test(s"${codec.mediaType}: lists and Unit union members round-trip") {
      assertEquals(read[GetForecastOutput](codec, write(codec, forecast)), forecast)
    }
  }

  test("the DynamicValue keeps Smithy member names, and unions become variants") {
    val codec = new MessagePackCodec
    val c     = summon[SchemaCodec[GetForecastOutput]]
    val value = codec.toDynamicValue(w => c.write(w, c.schema, GetForecastOutput(next = Some("t"), `type` = Kind.Rain(3))))
    value match {
      case DynamicValue.Record(fields) =>
        assertEquals(fields.map(_._1).toList, List("next", "type"))
        fields.toList(1)._2 match {
          case DynamicValue.Variant("rain", DynamicValue.Primitive(PrimitiveValue.Int(3))) => ()
          case other => fail(s"expected Variant(rain, 3), got $other")
        }
      case other => fail(s"expected a Record, got $other")
    }
  }

  test("YAML is readable text") {
    val yaml = new String(write(new YamlCodec, GetForecastOutput(next = Some("t"), `type` = Kind.Rain(3))), UTF_8)
    assert(yaml.contains("next: t"), yaml)
    assert(yaml.contains("rain: 3"), yaml)
  }

  test("ZIO Blocks JSON is plain JSON: member names as written, unions as one-key objects") {
    val json = new String(write(new ZioBlocksJsonCodec, GetForecastOutput(next = Some("t"), `type` = Kind.Rain(3))), UTF_8)
    assertEquals(json, """{"next":"t","type":{"rain":3}}""")
  }

  test("Thrift writes, but can't read a DynamicValue back") {
    val codec = new ThriftCodec
    val bytes = write(codec, table)
    assert(bytes.nonEmpty)
    intercept[SerializationException](read[TableDescription](codec, bytes)): Unit
  }
}
