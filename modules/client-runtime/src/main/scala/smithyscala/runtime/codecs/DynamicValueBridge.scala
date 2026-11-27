package smithyscala.runtime
package codecs

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64

import scala.collection.mutable.ArrayBuffer

import zio.blocks.chunk.Chunk
import zio.blocks.schema.DynamicValue
import zio.blocks.schema.PrimitiveValue

/**
  * The bridge between our shape codecs and ZIO Blocks: [[DynamicValueWriter]] turns `ShapeWriter` calls
  * into a `DynamicValue`, and [[DynamicValueReader]] reads one back through `ShapeReader`. A ZIO Blocks
  * format then turns the `DynamicValue` into bytes (see [[DynamicValueCodec]]).
  *
  * How Smithy values map:
  *
  * | Smithy                     | DynamicValue                                                  |
  * |----------------------------|---------------------------------------------------------------|
  * | structure                  | `Record`, member names as field names                         |
  * | union                      | `Variant(memberName, value)`                                  |
  * | list                       | `Sequence`                                                    |
  * | map                        | `Map` with `String` keys                                      |
  * | boolean, numbers, string   | the matching `PrimitiveValue`                                 |
  * | timestamp                  | ISO-8601 `PrimitiveValue.String`, like `date-time` in JSON    |
  * | blob                       | base64 `PrimitiveValue.String` (DynamicValue has no bytes)    |
  * | document                   | `Record` / `Sequence` / `Primitive` / `Null`                  |
  * | null                       | `Null`                                                        |
  *
  * Reading is lenient, because formats don't all keep what was written. YAML gives a `Variant` back
  * as a one-entry `Record` and re-types untagged scalars (the string `"1"` comes back as `Int(1)`).
  * MessagePack writes a `Variant` as a `{_case, _value}` map and gives `Int` back as `Long`. The
  * reader takes any shape that carries the value.
  */
private[codecs] final class DynamicValueWriter(put: (Schema, DynamicValue) => Unit) extends ShapeWriter {

  private def primitive(schema: Schema, value: PrimitiveValue): Unit = put(schema, DynamicValue.Primitive(value))

  def writeStruct(schema: Schema)(members: ShapeWriter => Unit): Unit = {
    val fields = ArrayBuffer.empty[(String, DynamicValue)]
    members(new DynamicValueWriter((member, value) => (fields += member.memberName -> value): Unit))
    val value =
      if (schema.shapeType == ShapeType.Union && fields.sizeIs == 1) DynamicValue.Variant(fields(0)._1, fields(0)._2)
      else DynamicValue.Record(Chunk.fromIterable(fields))
    put(schema, value)
  }

  def writeList(schema: Schema, size: Int)(elements: ShapeWriter => Unit): Unit = {
    val values = ArrayBuffer.empty[DynamicValue]
    elements(new DynamicValueWriter((_, value) => (values += value): Unit))
    put(schema, DynamicValue.Sequence(Chunk.fromIterable(values)))
  }

  def writeMap(schema: Schema, size: Int)(entries: MapWriter => Unit): Unit = {
    val pairs = ArrayBuffer.empty[(DynamicValue, DynamicValue)]
    entries(new MapWriter {
      def writeEntry(key: String)(value: ShapeWriter => Unit): Unit =
        value(new DynamicValueWriter((_, v) => (pairs += DynamicValueBridge.string(key) -> v): Unit))
    })
    put(schema, DynamicValue.Map(Chunk.fromIterable(pairs)))
  }

  def writeBoolean(schema: Schema, value: Boolean): Unit       = primitive(schema, PrimitiveValue.Boolean(value))
  def writeByte(schema: Schema, value: Byte): Unit             = primitive(schema, PrimitiveValue.Byte(value))
  def writeShort(schema: Schema, value: Short): Unit           = primitive(schema, PrimitiveValue.Short(value))
  def writeInteger(schema: Schema, value: Int): Unit           = primitive(schema, PrimitiveValue.Int(value))
  def writeLong(schema: Schema, value: Long): Unit             = primitive(schema, PrimitiveValue.Long(value))
  def writeFloat(schema: Schema, value: Float): Unit           = primitive(schema, PrimitiveValue.Float(value))
  def writeDouble(schema: Schema, value: Double): Unit         = primitive(schema, PrimitiveValue.Double(value))
  def writeBigInteger(schema: Schema, value: BigInt): Unit     = primitive(schema, PrimitiveValue.BigInt(value))
  def writeBigDecimal(schema: Schema, value: BigDecimal): Unit = primitive(schema, PrimitiveValue.BigDecimal(value))
  def writeString(schema: Schema, value: String): Unit         = primitive(schema, PrimitiveValue.String(value))

  /** ISO-8601 text: every format keeps a string, while `PrimitiveValue.Instant` comes back from YAML as `Instant(...)`. */
  def writeTimestamp(schema: Schema, value: Instant): Unit =
    primitive(schema, PrimitiveValue.String(DateTimeFormatter.ISO_INSTANT.format(value)))

  def writeBlob(schema: Schema, value: Blob): Unit =
    primitive(schema, PrimitiveValue.String(Base64.getEncoder.encodeToString(value.toArray)))

  def writeDocument(schema: Schema, value: Document): Unit = put(schema, DynamicValueBridge.fromDocument(value))
  def writeNull(schema: Schema): Unit                      = put(schema, DynamicValue.Null)
}

private[codecs] final class DynamicValueReader(value: DynamicValue) extends ShapeReader {

  private def fail(schema: Schema, expected: String): Nothing =
    throw new SerializationException(s"Expected $expected for ${schema.id}, got ${DynamicValueBridge.describe(value)}")

  def isNull: Boolean = value match {
    case null                                          => true
    case _: DynamicValue.Null.type                     => true
    case DynamicValue.Primitive(_: PrimitiveValue.Unit.type) => true
    case _                                             => false
  }

  /** The named entries of a structure, union or map, whichever way the format gave them back. */
  private def entries(schema: Schema): Seq[(String, DynamicValue)] = {
    val named = value match {
      case DynamicValue.Record(fields)   => fields.toSeq
      case DynamicValue.Variant(name, v) => Seq(name -> v)
      case DynamicValue.Map(pairs) =>
        pairs.toSeq.map {
          case (DynamicValue.Primitive(PrimitiveValue.String(key)), v) => key -> v
          case (key, _) => throw new SerializationException(s"Expected string keys for ${schema.id}, got ${DynamicValueBridge.describe(key)}")
        }
      case _ => fail(schema, "a structure or map")
    }
    // MessagePack's way of writing a Variant.
    if (schema.shapeType == ShapeType.Union) named match {
      case Seq(("_case", DynamicValue.Primitive(PrimitiveValue.String(name))), ("_value", v)) => Seq(name -> v)
      case _                                                                                   => named
    }
    else named
  }

  def readStruct(schema: Schema, onUnknown: String => Unit)(onMember: (Schema, ShapeReader) => Unit): Unit =
    entries(schema).foreach { (name, v) =>
      schema.member(name) match {
        case Some(member) => onMember(member, new DynamicValueReader(v))
        case None         => onUnknown(name)
      }
    }

  def readList(schema: Schema)(onElement: ShapeReader => Unit): Unit = value match {
    case DynamicValue.Sequence(elements) => elements.foreach(e => onElement(new DynamicValueReader(e)))
    case _                               => fail(schema, "a list")
  }

  def readMap(schema: Schema)(onEntry: (String, ShapeReader) => Unit): Unit =
    entries(schema).foreach((key, v) => onEntry(key, new DynamicValueReader(v)))

  private def primitive(schema: Schema, expected: String): PrimitiveValue = value match {
    case DynamicValue.Primitive(p) => p
    case _                         => fail(schema, expected)
  }

  def readBoolean(schema: Schema): Boolean = primitive(schema, "a boolean") match {
    case PrimitiveValue.Boolean(b)       => b
    case PrimitiveValue.String("true")   => true
    case PrimitiveValue.String("false")  => false
    case _                               => fail(schema, "a boolean")
  }

  private def number(schema: Schema): BigDecimal =
    DynamicValueBridge.number(primitive(schema, "a number")).getOrElse(fail(schema, "a number"))

  private def integral(schema: Schema): BigInt =
    number(schema).toBigIntExact.getOrElse(fail(schema, "an integer"))

  def readByte(schema: Schema): Byte           = integral(schema).toByte
  def readShort(schema: Schema): Short         = integral(schema).toShort
  def readInteger(schema: Schema): Int         = integral(schema).toInt
  def readLong(schema: Schema): Long           = integral(schema).toLong
  def readBigInteger(schema: Schema): BigInt   = integral(schema)
  def readBigDecimal(schema: Schema): BigDecimal = number(schema)
  def readFloat(schema: Schema): Float         = readDouble(schema).toFloat

  def readDouble(schema: Schema): Double = primitive(schema, "a number") match {
    case PrimitiveValue.Double(d)          => d
    case PrimitiveValue.Float(f)           => f.toDouble
    case PrimitiveValue.String("NaN")      => Double.NaN
    case PrimitiveValue.String("Infinity") => Double.PositiveInfinity
    case PrimitiveValue.String("-Infinity") => Double.NegativeInfinity
    case _                                 => number(schema).toDouble
  }

  def readString(schema: Schema): String = primitive(schema, "a string") match {
    case PrimitiveValue.String(s)  => s
    case PrimitiveValue.Char(c)    => c.toString
    // A format that re-types scalars (YAML) gives "1" back as a number and "true" as a boolean.
    case PrimitiveValue.Boolean(b) => b.toString
    case p => DynamicValueBridge.number(p).map(_.toString).getOrElse(fail(schema, "a string"))
  }

  def readBlob(schema: Schema): Blob = value match {
    case DynamicValue.Sequence(elements) =>
      Blob(elements.map(e => new DynamicValueReader(e).readByte(schema)).toArray)
    case _ =>
      try Blob(Base64.getDecoder.decode(readString(schema)))
      catch { case e: IllegalArgumentException => throw new SerializationException(s"Invalid base64 for ${schema.id}", e) }
  }

  def readTimestamp(schema: Schema): Instant = primitive(schema, "a timestamp") match {
    case PrimitiveValue.Instant(t)        => t
    case PrimitiveValue.OffsetDateTime(t) => t.toInstant
    case PrimitiveValue.ZonedDateTime(t)  => t.toInstant
    case PrimitiveValue.String(text) =>
      try OffsetDateTime.parse(text).toInstant
      catch { case e: DateTimeParseException => throw new SerializationException(s"Invalid timestamp '$text' for ${schema.id}", e) }
    case p => DynamicValueBridge.number(p).map(n => JsonCodec.fromEpochSeconds(n.bigDecimal)).getOrElse(fail(schema, "a timestamp"))
  }

  def readDocument(schema: Schema): Document = DynamicValueBridge.toDocument(value)
}

private[codecs] object DynamicValueBridge {

  def string(s: String): DynamicValue = DynamicValue.Primitive(PrimitiveValue.String(s))

  def number(p: PrimitiveValue): Option[BigDecimal] = p match {
    case PrimitiveValue.Byte(n)       => Some(BigDecimal(n.toInt))
    case PrimitiveValue.Short(n)      => Some(BigDecimal(n.toInt))
    case PrimitiveValue.Int(n)        => Some(BigDecimal(n))
    case PrimitiveValue.Long(n)       => Some(BigDecimal(n))
    case PrimitiveValue.Float(n) if n.isFinite  => Some(BigDecimal(n.toDouble))
    case PrimitiveValue.Double(n) if n.isFinite => Some(BigDecimal(n))
    case PrimitiveValue.BigInt(n)     => Some(BigDecimal(n))
    case PrimitiveValue.BigDecimal(n) => Some(n)
    case PrimitiveValue.String(s)     => s.toDoubleOption.map(_ => BigDecimal(s))
    case _                            => None
  }

  def fromDocument(doc: Document): DynamicValue = doc match {
    case Document.DNull       => DynamicValue.Null
    case Document.DBoolean(b) => DynamicValue.Primitive(PrimitiveValue.Boolean(b))
    case Document.DNumber(n)  => DynamicValue.Primitive(PrimitiveValue.BigDecimal(n))
    case Document.DString(s)  => string(s)
    case Document.DArray(vs)  => DynamicValue.Sequence(Chunk.fromIterable(vs.map(fromDocument)))
    case Document.DObject(fs) => DynamicValue.Record(Chunk.fromIterable(fs.map((k, v) => k -> fromDocument(v))))
  }

  def toDocument(value: DynamicValue): Document = value match {
    case DynamicValue.Primitive(PrimitiveValue.Boolean(b)) => Document.DBoolean(b)
    case DynamicValue.Primitive(PrimitiveValue.String(s))  => Document.DString(s)
    case DynamicValue.Primitive(PrimitiveValue.Instant(t)) => Document.DString(t.toString)
    case DynamicValue.Primitive(p) => number(p).fold(Document.DNull)(Document.DNumber(_))
    case DynamicValue.Record(fields)   => Document.DObject(fields.map((k, v) => k -> toDocument(v)).toMap)
    case DynamicValue.Variant(name, v) => Document.DObject(Map(name -> toDocument(v)))
    case DynamicValue.Sequence(vs)     => Document.DArray(vs.map(toDocument).toList)
    case DynamicValue.Map(pairs) =>
      Document.DObject(pairs.map((k, v) => toDocument(k) match {
        case Document.DString(s) => s -> toDocument(v)
        case other               => other.toString -> toDocument(v)
      }).toMap)
    case _ => Document.DNull
  }

  def describe(value: DynamicValue): String = value match {
    case null                        => "nothing"
    case DynamicValue.Primitive(p)   => p.getClass.getSimpleName
    case other                       => other.getClass.getSimpleName
  }
}
