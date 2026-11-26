package smithyscala.runtime

import java.io.ByteArrayOutputStream
import java.math.{BigDecimal as JBigDecimal}
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters.*

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode

/**
  * How a protocol wants JSON written.
  *
  * @param useJsonName
  *   honour `@jsonName` (restJson1 does; awsJson and rpcv2Json don't)
  * @param useTimestampFormat
  *   honour `@timestampFormat` (awsJson does; rpcv2Json always uses epoch seconds)
  */
final case class JsonSettings(useJsonName: Boolean, useTimestampFormat: Boolean)

/** The JSON codec: knows the format, nothing about HTTP. */
final class JsonCodec(val settings: JsonSettings) {
  private val factory = new JsonFactory()
  private val mapper  = new ObjectMapper(factory)
  private val nameIndex = new ConcurrentHashMap[Schema, Map[String, Schema]]()

  def serialize(write: ShapeWriter => Unit): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val gen = factory.createGenerator(out)
    try write(new JsonShapeWriter(gen, this, inStruct = false))
    finally gen.close()
    out.toByteArray
  }

  def deserialize[A](bytes: Array[Byte])(read: ShapeReader => A): A =
    read(new JsonShapeReader(readTree(bytes), this))

  /** Parses a body; an empty body reads as `{}`. */
  def readTree(bytes: Array[Byte]): JsonNode =
    if (bytes.isEmpty) mapper.createObjectNode()
    else
      try mapper.readTree(bytes)
      catch { case e: java.io.IOException => throw new SerializationException("Invalid JSON body", e) }

  def reader(node: JsonNode): ShapeReader = new JsonShapeReader(node, this)

  private[runtime] def fieldName(member: Schema): String =
    if (settings.useJsonName) member.traitValue(Traits.JsonName).getOrElse(member.memberName)
    else member.memberName

  private[runtime] def membersByFieldName(schema: Schema): Map[String, Schema] =
    nameIndex.computeIfAbsent(schema, s => s.members.map(m => fieldName(m) -> m).toMap)

  private[runtime] def timestampFormat(schema: Schema): String =
    if (settings.useTimestampFormat) schema.traitValue(Traits.TimestampFormat).getOrElse("epoch-seconds")
    else "epoch-seconds"
}

object JsonCodec {
  private[runtime] val HttpDate: DateTimeFormatter =
    DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

  private[runtime] def epochSeconds(t: Instant): String =
    JBigDecimal.valueOf(t.getEpochSecond).add(JBigDecimal.valueOf(t.getNano.toLong, 9)).stripTrailingZeros().toPlainString

  private[runtime] def fromEpochSeconds(value: JBigDecimal): Instant = {
    val seconds = value.setScale(0, java.math.RoundingMode.FLOOR)
    val nanos   = value.subtract(seconds).movePointRight(9).intValue()
    Instant.ofEpochSecond(seconds.longValueExact(), nanos.toLong)
  }
}

private final class JsonShapeWriter(gen: JsonGenerator, codec: JsonCodec, inStruct: Boolean) extends ShapeWriter {
  private lazy val structWriter = new JsonShapeWriter(gen, codec, inStruct = true)
  private lazy val valueWriter  = new JsonShapeWriter(gen, codec, inStruct = false)

  /** In a structure every value is a member, so its field name comes first. */
  private def name(schema: Schema): Unit =
    if (inStruct) gen.writeFieldName(codec.fieldName(schema))

  def writeStruct(schema: Schema)(members: ShapeWriter => Unit): Unit = {
    name(schema); gen.writeStartObject(); members(structWriter); gen.writeEndObject()
  }

  def writeList(schema: Schema, size: Int)(elements: ShapeWriter => Unit): Unit = {
    name(schema); gen.writeStartArray(); elements(valueWriter); gen.writeEndArray()
  }

  def writeMap(schema: Schema, size: Int)(entries: MapWriter => Unit): Unit = {
    name(schema)
    gen.writeStartObject()
    entries(new MapWriter {
      def writeEntry(key: String)(value: ShapeWriter => Unit): Unit = {
        gen.writeFieldName(key); value(valueWriter)
      }
    })
    gen.writeEndObject()
  }

  def writeBoolean(schema: Schema, value: Boolean): Unit = { name(schema); gen.writeBoolean(value) }
  def writeByte(schema: Schema, value: Byte): Unit       = { name(schema); gen.writeNumber(value.toInt) }
  def writeShort(schema: Schema, value: Short): Unit     = { name(schema); gen.writeNumber(value.toInt) }
  def writeInteger(schema: Schema, value: Int): Unit     = { name(schema); gen.writeNumber(value) }
  def writeLong(schema: Schema, value: Long): Unit       = { name(schema); gen.writeNumber(value) }

  def writeFloat(schema: Schema, value: Float): Unit = writeDouble(schema, value.toDouble)

  def writeDouble(schema: Schema, value: Double): Unit = {
    name(schema)
    // Smithy's JSON protocols send non-finite numbers as strings.
    if (value.isNaN) gen.writeString("NaN")
    else if (value.isPosInfinity) gen.writeString("Infinity")
    else if (value.isNegInfinity) gen.writeString("-Infinity")
    else gen.writeNumber(value)
  }

  def writeBigInteger(schema: Schema, value: BigInt): Unit = { name(schema); gen.writeNumber(value.bigInteger) }
  def writeBigDecimal(schema: Schema, value: BigDecimal): Unit = {
    name(schema); gen.writeNumber(value.bigDecimal.toPlainString)
  }
  def writeString(schema: Schema, value: String): Unit = { name(schema); gen.writeString(value) }
  def writeBlob(schema: Schema, value: Blob): Unit = {
    name(schema); gen.writeString(Base64.getEncoder.encodeToString(value.toArray))
  }

  def writeTimestamp(schema: Schema, value: Instant): Unit = {
    name(schema)
    codec.timestampFormat(schema) match {
      case "date-time" => gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value))
      case "http-date" => gen.writeString(JsonCodec.HttpDate.format(value))
      case _           => gen.writeNumber(JsonCodec.epochSeconds(value))
    }
  }

  def writeDocument(schema: Schema, value: Document): Unit = { name(schema); writeDoc(value) }

  private def writeDoc(value: Document): Unit = value match {
    case Document.DNull        => gen.writeNull()
    case Document.DBoolean(b)  => gen.writeBoolean(b)
    case Document.DNumber(n)   => gen.writeNumber(n.bigDecimal)
    case Document.DString(s)   => gen.writeString(s)
    case Document.DArray(vs)   => gen.writeStartArray(); vs.foreach(writeDoc); gen.writeEndArray()
    case Document.DObject(fs) =>
      gen.writeStartObject()
      fs.foreach { (k, v) => gen.writeFieldName(k); writeDoc(v) }
      gen.writeEndObject()
  }

  def writeNull(schema: Schema): Unit = { name(schema); gen.writeNull() }
}

private final class JsonShapeReader(node: JsonNode, codec: JsonCodec) extends ShapeReader {

  def isNull: Boolean = (node eq null) || node.isNull || node.isMissingNode

  private def fail(schema: Schema, expected: String): Nothing =
    throw new SerializationException(s"Expected $expected for ${schema.id}, got ${node.getNodeType}")

  def readStruct(schema: Schema, onUnknown: String => Unit)(onMember: (Schema, ShapeReader) => Unit): Unit = {
    if (!node.isObject) fail(schema, "an object")
    val byName = codec.membersByFieldName(schema)
    node.properties().asScala.foreach { entry =>
      val key = entry.getKey
      byName.get(key) match {
        case Some(member) => onMember(member, new JsonShapeReader(entry.getValue, codec))
        case None         => if (key != "__type") onUnknown(key)
      }
    }
  }

  def readList(schema: Schema)(onElement: ShapeReader => Unit): Unit = {
    if (!node.isArray) fail(schema, "an array")
    node.elements().asScala.foreach(e => onElement(new JsonShapeReader(e, codec)))
  }

  def readMap(schema: Schema)(onEntry: (String, ShapeReader) => Unit): Unit = {
    if (!node.isObject) fail(schema, "an object")
    node.properties().asScala.foreach(e => onEntry(e.getKey, new JsonShapeReader(e.getValue, codec)))
  }

  def readBoolean(schema: Schema): Boolean = if (node.isBoolean) node.booleanValue else fail(schema, "a boolean")

  private def integral(schema: Schema): JsonNode = if (node.isIntegralNumber) node else fail(schema, "an integer")
  def readByte(schema: Schema): Byte     = integral(schema).intValue.toByte
  def readShort(schema: Schema): Short   = integral(schema).intValue.toShort
  def readInteger(schema: Schema): Int   = integral(schema).intValue
  def readLong(schema: Schema): Long     = integral(schema).longValue
  def readFloat(schema: Schema): Float   = readDouble(schema).toFloat

  def readDouble(schema: Schema): Double =
    if (node.isNumber) node.doubleValue
    else if (node.isTextual) node.textValue match {
      case "NaN"       => Double.NaN
      case "Infinity"  => Double.PositiveInfinity
      case "-Infinity" => Double.NegativeInfinity
      case _           => fail(schema, "a number")
    }
    else fail(schema, "a number")

  def readBigInteger(schema: Schema): BigInt = BigInt(integral(schema).bigIntegerValue)
  def readBigDecimal(schema: Schema): BigDecimal =
    if (node.isNumber) BigDecimal(node.decimalValue) else fail(schema, "a number")
  def readString(schema: Schema): String = if (node.isTextual) node.textValue else fail(schema, "a string")

  def readBlob(schema: Schema): Blob =
    try Blob(Base64.getDecoder.decode(readString(schema)))
    catch { case e: IllegalArgumentException => throw new SerializationException(s"Invalid base64 for ${schema.id}", e) }

  def readTimestamp(schema: Schema): Instant =
    if (node.isNumber) JsonCodec.fromEpochSeconds(node.decimalValue)
    else if (node.isTextual) {
      val text = node.textValue
      try {
        if (codec.timestampFormat(schema) == "http-date") Instant.from(JsonCodec.HttpDate.parse(text))
        else java.time.OffsetDateTime.parse(text).toInstant
      } catch {
        case e: java.time.format.DateTimeParseException =>
          throw new SerializationException(s"Invalid timestamp '$text' for ${schema.id}", e)
      }
    } else fail(schema, "a timestamp")

  def readDocument(schema: Schema): Document = toDocument(node)

  private def toDocument(n: JsonNode): Document =
    if ((n eq null) || n.isNull || n.isMissingNode) Document.DNull
    else if (n.isBoolean) Document.DBoolean(n.booleanValue)
    else if (n.isNumber) Document.DNumber(BigDecimal(n.decimalValue))
    else if (n.isTextual) Document.DString(n.textValue)
    else if (n.isArray) Document.DArray(n.elements().asScala.map(toDocument).toList)
    else Document.DObject(n.properties().asScala.map(e => e.getKey -> toDocument(e.getValue)).toMap)
}

private[runtime] object JsonNodes {
  val Null: JsonNode = NullNode.getInstance()
}
