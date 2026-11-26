package smithyscala.runtime

import smithyscala.runtime.codecs.*

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

class JsonCodecSpec extends munit.FunSuite {
  private val id = ShapeId("example", "Item")
  private val schema = Schema.aggregate(id, ShapeType.Structure)(
    MemberDef("Name", () => Prelude.String, Traits(Traits.JsonName -> "name")),
    MemberDef("At", () => Prelude.Timestamp, Traits(Traits.TimestampFormat -> "date-time")),
    MemberDef("Ratio", () => Prelude.Double),
    MemberDef("Data", () => Prelude.Blob)
  )

  private def write(codec: JsonCodec): String =
    new String(
      codec.serialize { w =>
        w.writeStruct(schema) { s =>
          s.writeString(schema.members(0), "a")
          s.writeTimestamp(schema.members(1), Instant.parse("2026-11-26T10:00:00.5Z"))
          s.writeDouble(schema.members(2), Double.NaN)
          s.writeBlob(schema.members(3), Blob.fromUtf8("hi"))
        }
      },
      UTF_8
    )

  test("awsJson settings: member names, @timestampFormat honoured") {
    assertEquals(
      write(new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = true))),
      """{"Name":"a","At":"2026-11-26T10:00:00.500Z","Ratio":"NaN","Data":"aGk="}"""
    )
  }

  test("restJson-style settings: @jsonName honoured; rpcv2Json: epoch seconds") {
    assertEquals(
      write(new JsonCodec(JsonSettings(useJsonName = true, useTimestampFormat = false))),
      """{"name":"a","At":1795687200.5,"Ratio":"NaN","Data":"aGk="}"""
    )
  }

  test("reading skips unknown fields and reports them") {
    val codec   = new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = true))
    var seen    = Map.empty[String, String]
    var unknown = List.empty[String]
    codec.deserialize("""{"Name":"x","Extra":1,"__type":"t","At":1.25}""".getBytes(UTF_8)) { r =>
      r.readStruct(schema, n => unknown = n :: unknown) { (m, mr) =>
        if (m.memberName == "Name") seen += "Name" -> mr.readString(m)
        else seen += m.memberName -> mr.readTimestamp(m).toString
      }
    }
    assertEquals(seen, Map("Name" -> "x", "At" -> "1970-01-01T00:00:01.250Z"))
    assertEquals(unknown, List("Extra"))
  }
}
