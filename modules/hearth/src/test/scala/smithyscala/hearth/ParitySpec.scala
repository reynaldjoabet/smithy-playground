package smithyscala.hearth

import smithyscala.runtime.codecs.*

import java.nio.charset.StandardCharsets.UTF_8

import smithyscala.runtime.*

/**
  * The two derivations side by side on the same generated types: `example.*` uses the runtime's Mirror derivation,
  * `example.hearth.*` was generated with `--derivation=hearth`. Both must write the same JSON and read each other's.
  */
class ParitySpec extends munit.FunSuite {
  private val json = new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = false))

  private def write[A](value: A)(using c: SchemaCodec[A]): String =
    new String(json.serialize(w => c.write(w, c.schema, value)), UTF_8)

  private def read[A](text: String)(using c: SchemaCodec[A]): A =
    json.deserialize(text.getBytes(UTF_8))(r => c.read(r, c.schema))

  test("DynamoDB: a recursive AttributeValue item") {
    import example.dynamodb as m
    import example.hearth.dynamodb as h
    val mirror = m.PutItemInput("t", Map(
      "pk"   -> m.AttributeValue.S("a"),
      "tags" -> m.AttributeValue.L(List(m.AttributeValue.N("1"), m.AttributeValue.NULL(true))),
      "meta" -> m.AttributeValue.M(Map("ok" -> m.AttributeValue.BOOL(true), "b" -> m.AttributeValue.B(Blob.fromUtf8("hi"))))
    ))
    val hearth = h.PutItemInput("t", Map(
      "pk"   -> h.AttributeValue.S("a"),
      "tags" -> h.AttributeValue.L(List(h.AttributeValue.N("1"), h.AttributeValue.NULL(true))),
      "meta" -> h.AttributeValue.M(Map("ok" -> h.AttributeValue.BOOL(true), "b" -> h.AttributeValue.B(Blob.fromUtf8("hi"))))
    ))
    assertEquals(write(hearth), write(mirror))
    assertEquals(read[h.PutItemInput](write(mirror)), hearth)
    assertEquals(read[m.PutItemInput](write(hearth)), mirror)
  }

  test("DynamoDB: unknown union members and missing required members") {
    import example.dynamodb as m
    import example.hearth.dynamodb as h
    val input = """{"Item":{"x":{"NEW":1}}}"""
    assertEquals(read[h.GetItemOutput](input).item.flatMap(_.get("x")), Some(h.AttributeValue.UnknownMember("NEW")))
    assertEquals(read[m.GetItemOutput](input).item.flatMap(_.get("x")), Some(m.AttributeValue.UnknownMember("NEW")))
    val missing = """{"IndexName":"i","KeySchema":[]}"""
    val hErr = intercept[SerializationException](read[h.GlobalSecondaryIndex](missing)).getMessage
    val mErr = intercept[SerializationException](read[m.GlobalSecondaryIndex](missing)).getMessage
    assertEquals(hErr, mErr)
  }

  test("Weather: defaults, sparse lists, Unit union members, timestamps") {
    import example.weather as m
    import example.hearth.weather as h
    assertEquals(write(h.GetForecastInput("Oslo")), write(m.GetForecastInput("Oslo")))
    val output = """{"days":[{"date":1795687200,"copy":"c"},null],"type":{"sunny":{}},"next":null}"""
    assertEquals(write(read[h.GetForecastOutput](output)), write(read[m.GetForecastOutput](output)))
    assertEquals(read[h.GetForecastOutput]("{}").`type`, h.Kind.UnknownMember(""))
    assertEquals(h.Units.knownValues.map(_.value), m.Units.knownValues.map(_.value))
    assertEquals(read[h.GetForecastInput]("""{"city":"Oslo"}""").units, h.Units.METRIC)
  }
}
