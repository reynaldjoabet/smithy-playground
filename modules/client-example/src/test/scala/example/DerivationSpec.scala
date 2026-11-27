package example

import smithy.scala.runtime.codecs.*

import java.nio.charset.StandardCharsets.UTF_8

import smithy.scala.runtime.*

import example.dynamodb.AttributeValue
import example.weather.*

/** The compile-time pieces on the generated clients: derived codecs (Hearth, the default), enum values, TypeRegistry.of. */
class DerivationSpec extends munit.FunSuite {
  private val codec = new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = false))

  private def read[A](json: String)(using c: SchemaCodec[A]): A =
    codec.deserialize(json.getBytes(UTF_8))(r => c.read(r, c.schema))

  private def write[A](value: A)(using c: SchemaCodec[A]): String =
    new String(codec.serialize(w => c.write(w, c.schema, value)), UTF_8)

  test("missing @default members get the case class default") {
    assertEquals(read[GetForecastInput]("""{"city":"Oslo"}"""), GetForecastInput("Oslo", units = Units.METRIC, detail = Detail.LOW))
    // The runtime's Defaults macro (used by the Mirror derivation) reads the same defaults.
    assertEquals(Defaults.of[GetForecastInput], Map(1 -> None, 2 -> None, 3 -> Units.METRIC, 4 -> Detail.LOW))
  }

  test("a missing required member gets a zero value; a missing required structure fails") {
    assertEquals(read[GetForecastOutput]("{}"), GetForecastOutput(`type` = Kind.UnknownMember("")))
    // Put.Item is a required map: it error-corrects to empty.
    assertEquals(read[example.dynamodb.Put]("""{"TableName":"t"}""").item, Map.empty)
    // GlobalSecondaryIndex.Projection is a required structure: there's no zero value for it.
    val e = intercept[SerializationException](read[example.dynamodb.GlobalSecondaryIndex]("""{"IndexName":"i","KeySchema":[]}"""))
    assert(e.getMessage.contains("GlobalSecondaryIndex$Projection"), e.getMessage)
  }

  test("enum values are found at compile time: singletons only, in declaration order") {
    assertEquals(Units.knownValues, List(Units.METRIC, Units.IMPERIAL))
    assertEquals(Detail.knownValues.map(_.value), List(1, 2))
    assertEquals(Units.fromValue("KELVIN"), Units.UnknownValue("KELVIN"))
  }

  test("union cases map to members by ordinal, Unit members included") {
    assertEquals(write[Kind](Kind.Sunny), """{"sunny":{}}""")
    assertEquals(write[Kind](Kind.Rain(3)), """{"rain":3}""")
    assertEquals(read[Kind]("""{"hail":true}"""), Kind.UnknownMember("hail"))
    intercept[SerializationException](write[Kind](Kind.UnknownMember("hail"))): Unit
  }

  test("recursive unions round-trip") {
    val v = AttributeValue.M(Map("l" -> AttributeValue.L(List(AttributeValue.N("1"), AttributeValue.M(Map.empty)))))
    assertEquals(read[AttributeValue](write[AttributeValue](v)), v)
  }

  test("TypeRegistry.of rejects a type that isn't a ServiceException at compile time") {
    val errors = compileErrors("smithy.scala.runtime.TypeRegistry.of[java.lang.String *: EmptyTuple]")
    assert(errors.contains("every error type must extend ServiceException"), errors)
    assert(TypeRegistry.of[Busy *: EmptyTuple].find("example.weather#Busy").isDefined)
  }
}
