package smithy.scala.runtime
package codecs

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.jdk.CollectionConverters.*

import org.apache.avro.AvroRuntimeException
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema.Field
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.Encoder
import org.apache.avro.io.EncoderFactory
import org.apache.avro.{Schema as AvroSchema}

/**
  * Avro binary for the shape itself, with Apache Avro's encoder: the record layout comes from our
  * runtime [[Schema]], as [[AvroSchemas.of]] describes it. Unlike [[AvroCodec]] (ZIO Blocks' generic
  * `DynamicValue` schema), the bytes hold values only, no field names, and any Avro tool can read them
  * with the schema from [[AvroSchemas.of]].
  *
  * Avro has no tags: fields are written one after another in schema order, and every field is written.
  * The writer streams, so the shape codec must write a structure's members in member order (both
  * derivations do); members it skips are written as the `null` branch. The reader is the same in
  * reverse, and the shape codec must read every member it is handed.
  *
  * Writer and reader must use the same schema, i.e. the same model version. Reading bytes written
  * with an older schema needs Avro's schema resolution (`ResolvingDecoder`), which this doesn't do.
  */
final class AvroSchemaCodec extends Codec {
  val mediaType: String = "application/avro"

  def serialize(write: ShapeWriter => Unit): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val enc = EncoderFactory.get().binaryEncoder(out, null)
    write(new AvroShapeWriter(enc, new AvroValueSlot(nullable = false, inCollection = false)))
    enc.flush()
    out.toByteArray
  }

  def deserialize[A](bytes: Array[Byte])(read: ShapeReader => A): A = {
    val dec = DecoderFactory.get().binaryDecoder(bytes, null)
    try read(new AvroShapeReader(dec, nullable = false))
    catch {
      case e: IOException          => throw new SerializationException(s"Invalid Avro body: $e", e)
      case e: AvroRuntimeException => throw new SerializationException(s"Invalid Avro body: $e", e)
    }
  }
}

/**
  * The Avro schema for a shape. Every type here has to match what [[AvroSchemaCodec]] writes:
  *
  * | Smithy                      | Avro                                                                 |
  * |-----------------------------|----------------------------------------------------------------------|
  * | structure                   | `record`, named after the shape; every field is `["null", T]`, default `null` |
  * | union                       | `record` with one field `member`: a union of `<Union>_<member>` records, each with one field `value` |
  * | list / map                  | `array` / `map` (`["null", T]` items when `@sparse`)                  |
  * | boolean, string, blob       | `boolean`, `string`, `bytes`                                         |
  * | byte, short, integer, intEnum | `int`                                                              |
  * | long, float, double         | `long`, `float`, `double`                                            |
  * | bigInteger, bigDecimal      | `string` (Avro's `decimal` needs a fixed scale; Smithy has none)     |
  * | enum                        | `string` (Smithy enums are open; an Avro `enum` would reject new values) |
  * | timestamp                   | `long` with logical type `timestamp-micros`                          |
  * | document                    | `string` holding JSON                                                |
  *
  * Members are always nullable, even `@required` ones, so a writer never has to invent a value, and
  * a schema with an added member can still read old data. A union is wrapped in a record because Avro
  * unions are unnamed, and a recursive union (DynamoDB's `AttributeValue`) needs a name to refer back to.
  */
object AvroSchemas {
  def of(schema: Schema): AvroSchema = new Builder().build(schema)

  private[codecs] def target(schema: Schema): Schema = schema match {
    case m: MemberSchema => target(m.target)
    case s               => s
  }

  private final class Builder {
    private val named = scala.collection.mutable.Map.empty[ShapeId, AvroSchema]

    private def nullable(s: AvroSchema): AvroSchema =
      AvroSchema.createUnion(AvroSchema.create(AvroSchema.Type.NULL), s)

    def build(schema: Schema): AvroSchema = {
      val t = target(schema)
      t.shapeType match {
        case ShapeType.Boolean                                         => AvroSchema.create(AvroSchema.Type.BOOLEAN)
        case ShapeType.Byte | ShapeType.Short | ShapeType.Integer | ShapeType.IntEnum => AvroSchema.create(AvroSchema.Type.INT)
        case ShapeType.Long                                            => AvroSchema.create(AvroSchema.Type.LONG)
        case ShapeType.Float                                           => AvroSchema.create(AvroSchema.Type.FLOAT)
        case ShapeType.Double                                          => AvroSchema.create(AvroSchema.Type.DOUBLE)
        case ShapeType.BigInteger | ShapeType.BigDecimal | ShapeType.String | ShapeType.Enum | ShapeType.Document =>
          AvroSchema.create(AvroSchema.Type.STRING)
        case ShapeType.Blob      => AvroSchema.create(AvroSchema.Type.BYTES)
        case ShapeType.Timestamp => LogicalTypes.timestampMicros().addToSchema(AvroSchema.create(AvroSchema.Type.LONG))
        case ShapeType.List =>
          val item = build(t.members(0))
          AvroSchema.createArray(if (schema.hasTrait(Traits.Sparse)) nullable(item) else item)
        case ShapeType.Map =>
          val value = build(t.members(1))
          AvroSchema.createMap(if (schema.hasTrait(Traits.Sparse)) nullable(value) else value)
        case ShapeType.Structure =>
          record(t) { rec =>
            t.members.map(m => new Field(m.memberName, nullable(build(m)), null, Field.NULL_DEFAULT_VALUE)).toList
          }
        case ShapeType.Union =>
          record(t) { rec =>
            val branches = t.members.map { m =>
              val b = AvroSchema.createRecord(s"${t.id.name}_${m.memberName}", null, t.id.namespace, false)
              b.setFields(List(new Field("value", build(m))).asJava)
              b
            }
            List(new Field("member", AvroSchema.createUnion(branches.asJava)))
          }
      }
    }

    /** Registers the record before building its fields, so recursive shapes refer back to it by name. */
    private def record(t: Schema)(fields: AvroSchema => List[Field]): AvroSchema =
      named.getOrElse(
        t.id, {
          val rec = AvroSchema.createRecord(t.id.name, null, t.id.namespace, false)
          named.update(t.id, rec)
          rec.setFields(fields(rec).asJava)
          rec
        }
      )
  }
}

// ------------------------------------------------------------------------------------------------ writing

/** Where the next value goes: what to write before it, and how to write a null. */
private sealed trait AvroSlot {
  def before(enc: Encoder, schema: Schema): Unit
  def writeNull(enc: Encoder, schema: Schema): Unit
}

/** A value on its own: the top level, a list item or a map value. */
private final class AvroValueSlot(nullable: Boolean, inCollection: Boolean) extends AvroSlot {
  var count = 0

  def before(enc: Encoder, schema: Schema): Unit = {
    if (inCollection) { enc.startItem(); count += 1 }
    if (nullable) enc.writeIndex(1)
  }

  def writeNull(enc: Encoder, schema: Schema): Unit = {
    if (!nullable) throw new SerializationException(s"Avro: null for ${schema.id}, which isn't @sparse")
    if (inCollection) { enc.startItem(); count += 1 }
    enc.writeIndex(0)
    enc.writeNull()
  }
}

/** The members of a structure: each is `["null", T]`, written in order, with nulls for the gaps. */
private final class AvroStructSlot(container: Schema) extends AvroSlot {
  private var next = 0

  private def fillTo(index: Int, enc: Encoder): Unit = {
    if (index < next) throw new SerializationException(s"Avro: members of ${container.id} written out of order")
    while (next < index) { enc.writeIndex(0); enc.writeNull(); next += 1 }
  }

  def before(enc: Encoder, schema: Schema): Unit = { fillTo(schema.memberIndex, enc); enc.writeIndex(1); next += 1 }
  def writeNull(enc: Encoder, schema: Schema): Unit = { fillTo(schema.memberIndex, enc); enc.writeIndex(0); enc.writeNull(); next += 1 }
  def finish(enc: Encoder): Unit = fillTo(container.members.size, enc)
}

/** A union: exactly one member, written as its branch index then its value. */
private final class AvroUnionSlot(container: Schema) extends AvroSlot {
  private var written = false

  def before(enc: Encoder, schema: Schema): Unit = {
    if (written) throw new SerializationException(s"Avro: more than one member set on union ${container.id}")
    enc.writeIndex(schema.memberIndex)
    written = true
  }
  def writeNull(enc: Encoder, schema: Schema): Unit =
    throw new SerializationException(s"Avro: null member in union ${container.id}")
  def finish(): Unit =
    if (!written) throw new SerializationException(s"Avro: no member set on union ${container.id}")
}

private final class AvroShapeWriter(enc: Encoder, slot: AvroSlot) extends ShapeWriter {

  def writeStruct(schema: Schema)(members: ShapeWriter => Unit): Unit = {
    slot.before(enc, schema)
    if (schema.shapeType == ShapeType.Union) {
      val inner = new AvroUnionSlot(schema)
      members(new AvroShapeWriter(enc, inner))
      inner.finish()
    } else {
      val inner = new AvroStructSlot(schema)
      members(new AvroShapeWriter(enc, inner))
      inner.finish(enc)
    }
  }

  def writeList(schema: Schema, size: Int)(elements: ShapeWriter => Unit): Unit = {
    slot.before(enc, schema)
    val items = new AvroValueSlot(schema.hasTrait(Traits.Sparse), inCollection = true)
    enc.writeArrayStart()
    enc.setItemCount(size.toLong)
    elements(new AvroShapeWriter(enc, items))
    if (items.count != size) throw new SerializationException(s"Avro: ${schema.id} announced $size items, wrote ${items.count}")
    enc.writeArrayEnd()
  }

  def writeMap(schema: Schema, size: Int)(entries: MapWriter => Unit): Unit = {
    slot.before(enc, schema)
    val sparse = schema.hasTrait(Traits.Sparse)
    var count  = 0
    enc.writeMapStart()
    enc.setItemCount(size.toLong)
    entries(new MapWriter {
      def writeEntry(key: String)(value: ShapeWriter => Unit): Unit = {
        enc.startItem()
        count += 1
        enc.writeString(key)
        value(new AvroShapeWriter(enc, new AvroValueSlot(sparse, inCollection = false)))
      }
    })
    if (count != size) throw new SerializationException(s"Avro: ${schema.id} announced $size entries, wrote $count")
    enc.writeMapEnd()
  }

  def writeBoolean(schema: Schema, value: Boolean): Unit = { slot.before(enc, schema); enc.writeBoolean(value) }
  def writeByte(schema: Schema, value: Byte): Unit       = { slot.before(enc, schema); enc.writeInt(value.toInt) }
  def writeShort(schema: Schema, value: Short): Unit     = { slot.before(enc, schema); enc.writeInt(value.toInt) }
  def writeInteger(schema: Schema, value: Int): Unit     = { slot.before(enc, schema); enc.writeInt(value) }
  def writeLong(schema: Schema, value: Long): Unit       = { slot.before(enc, schema); enc.writeLong(value) }
  def writeFloat(schema: Schema, value: Float): Unit     = { slot.before(enc, schema); enc.writeFloat(value) }
  def writeDouble(schema: Schema, value: Double): Unit   = { slot.before(enc, schema); enc.writeDouble(value) }
  def writeBigInteger(schema: Schema, value: BigInt): Unit     = { slot.before(enc, schema); enc.writeString(value.toString) }
  def writeBigDecimal(schema: Schema, value: BigDecimal): Unit = { slot.before(enc, schema); enc.writeString(value.bigDecimal.toString) }
  def writeString(schema: Schema, value: String): Unit   = { slot.before(enc, schema); enc.writeString(value) }
  def writeBlob(schema: Schema, value: Blob): Unit       = { slot.before(enc, schema); enc.writeBytes(value.toArray) }

  def writeTimestamp(schema: Schema, value: Instant): Unit = {
    slot.before(enc, schema)
    enc.writeLong(ChronoUnit.MICROS.between(Instant.EPOCH, value))
  }

  def writeDocument(schema: Schema, value: Document): Unit = {
    slot.before(enc, schema)
    enc.writeString(new String(AvroShapeWriter.json.serialize(_.writeDocument(Prelude.Document, value)), "UTF-8"))
  }

  def writeNull(schema: Schema): Unit = slot.writeNull(enc, schema)
}

private object AvroShapeWriter {
  /** Documents are written as a JSON string. */
  val json: JsonCodec = new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = false))
}

// ------------------------------------------------------------------------------------------------ reading

/** Reads one value. A nullable one (a `@sparse` item) reads its union branch first. */
private final class AvroShapeReader(dec: BinaryDecoder, nullable: Boolean) extends ShapeReader {

  private val present: Boolean =
    !nullable || (dec.readIndex() match {
      case 0 => dec.readNull(); false
      case _ => true
    })

  def isNull: Boolean = !present

  def readStruct(schema: Schema, onUnknown: String => Unit)(onMember: (Schema, ShapeReader) => Unit): Unit =
    if (schema.shapeType == ShapeType.Union) {
      val index = dec.readIndex()
      if (index < 0 || index >= schema.members.size) throw new SerializationException(s"Avro: no member $index in union ${schema.id}")
      onMember(schema.members(index), new AvroShapeReader(dec, nullable = false))
    } else
      schema.members.foreach { member =>
        dec.readIndex() match {
          case 0 => dec.readNull()
          case _ => onMember(member, new AvroShapeReader(dec, nullable = false))
        }
      }

  def readList(schema: Schema)(onElement: ShapeReader => Unit): Unit = {
    val sparse = schema.hasTrait(Traits.Sparse)
    var n      = dec.readArrayStart()
    while (n > 0) {
      var i = 0L
      while (i < n) { onElement(new AvroShapeReader(dec, sparse)); i += 1 }
      n = dec.arrayNext()
    }
  }

  def readMap(schema: Schema)(onEntry: (String, ShapeReader) => Unit): Unit = {
    val sparse = schema.hasTrait(Traits.Sparse)
    var n      = dec.readMapStart()
    while (n > 0) {
      var i = 0L
      while (i < n) {
        val key = dec.readString()
        onEntry(key, new AvroShapeReader(dec, sparse))
        i += 1
      }
      n = dec.mapNext()
    }
  }

  def readBoolean(schema: Schema): Boolean       = dec.readBoolean()
  def readByte(schema: Schema): Byte             = dec.readInt().toByte
  def readShort(schema: Schema): Short           = dec.readInt().toShort
  def readInteger(schema: Schema): Int           = dec.readInt()
  def readLong(schema: Schema): Long             = dec.readLong()
  def readFloat(schema: Schema): Float           = dec.readFloat()
  def readDouble(schema: Schema): Double         = dec.readDouble()
  def readBigInteger(schema: Schema): BigInt     = BigInt(dec.readString())
  def readBigDecimal(schema: Schema): BigDecimal = BigDecimal(dec.readString())
  def readString(schema: Schema): String         = dec.readString()

  def readBlob(schema: Schema): Blob = {
    val buffer = dec.readBytes(null)
    val bytes  = new Array[Byte](buffer.remaining())
    buffer.get(bytes): Unit
    Blob(bytes)
  }

  def readTimestamp(schema: Schema): Instant = Instant.EPOCH.plus(dec.readLong(), ChronoUnit.MICROS)

  def readDocument(schema: Schema): Document =
    AvroShapeWriter.json.deserialize(dec.readString().getBytes("UTF-8"))(_.readDocument(Prelude.Document))
}
