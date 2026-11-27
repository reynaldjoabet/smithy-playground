package smithy.scala.hearth

import smithy.scala.runtime.codecs.*

import java.nio.charset.StandardCharsets.UTF_8

import smithy.scala.runtime.*

/** What the Hearth derivation promises beyond the generated clients: overrides, recursion, error reports. */
class HearthDerivationSpec extends munit.FunSuite {
  import HearthDerivationSpec.*

  private val json = new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = false))

  private def write[A](value: A)(using c: SchemaCodec[A]): String =
    new String(json.serialize(w => c.write(w, c.schema, value)), UTF_8)

  private def read[A](text: String)(using c: SchemaCodec[A]): A =
    json.deserialize(text.getBytes(UTF_8))(r => c.read(r, c.schema))

  test("a codec the user provides for a field type is used instead of deriving one") {
    assertEquals(write(Reading("kitchen", Celsius(21.5))), """{"room":"kitchen","temperature":"21.5C"}""")
    assertEquals(read[Reading]("""{"room":"hall","temperature":"18.0C"}"""), Reading("hall", Celsius(18.0)))
  }

  test("a recursive case class derives with no tricks (a cached helper def calls itself)") {
    val tree = Tree("root", List(Tree("a", Nil), Tree("b", List(Tree("b1", Nil)))))
    assertEquals(read[Tree](write(tree)), tree)
    assertEquals(write(Tree("leaf", Nil)), """{"name":"leaf","children":[]}""")
  }

  test("every unsupported field is reported at once, with the reasons and how to see the derivation log") {
    val errors = compileErrors(
      """smithy.scala.hearth.HearthCodec.derived[smithy.scala.hearth.HearthDerivationSpec.Unsupported](smithy.scala.runtime.Prelude.Unit)"""
    )
    assert(errors.contains("HearthCodec.derived failed for smithy.scala.hearth.HearthDerivationSpec.Unsupported"), errors)
    assert(errors.contains("Smithy maps have String keys, but this one has scala.Int"), errors)
    assert(errors.contains("No rule could derive a codec for java.util.UUID"), errors)
    assert(errors.contains("import smithy.scala.hearth.debug.logDerivation"), errors)
  }
}

object HearthDerivationSpec {
  final case class Celsius(degrees: Double)

  /** Written as a string such as "21.5C", which derivation would never produce for a case class. */
  given ShapeCodec[Celsius] with {
    def write(w: ShapeWriter, s: Schema, value: Celsius): Unit = w.writeString(s, s"${value.degrees}C")
    def read(r: ShapeReader, s: Schema): Celsius               = Celsius(r.readString(s).stripSuffix("C").toDouble)
  }

  final case class Reading(room: String, temperature: Celsius)
  object Reading {
    lazy val schema: Schema = Schema.aggregate(ShapeId("test", "Reading"), ShapeType.Structure)(
      MemberDef("room", () => Prelude.String),
      MemberDef("temperature", () => Prelude.String)
    )
    given codec: SchemaCodec[Reading] = HearthCodec.derived[Reading](schema)
  }

  final case class Tree(name: String, children: List[Tree])
  object Tree {
    lazy val schema: Schema = Schema.aggregate(ShapeId("test", "Tree"), ShapeType.Structure)(
      MemberDef("name", () => Prelude.String),
      MemberDef("children", () => Trees)
    )
    lazy val Trees: Schema = Schema.aggregate(ShapeId("test", "Trees"), ShapeType.List)(MemberDef("member", () => schema))
    given codec: SchemaCodec[Tree] = HearthCodec.derived[Tree](schema)
  }

  final case class Unsupported(byNumber: Map[Int, String], id: java.util.UUID)
}
