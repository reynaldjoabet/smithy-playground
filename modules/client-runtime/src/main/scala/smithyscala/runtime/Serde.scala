package smithyscala.runtime

import java.time.Instant

/**
  * Format-neutral serialization. Generated code walks its members and calls these methods with
  * each member's schema; a codec decides what the value looks like in its format. This is the
  * same split as smithy-java's `ShapeSerializer` and smithy-go's `ShapeSerializer`.
  */
trait ShapeWriter {
  def writeStruct(schema: Schema)(members: ShapeWriter => Unit): Unit
  def writeList(schema: Schema, size: Int)(elements: ShapeWriter => Unit): Unit
  def writeMap(schema: Schema, size: Int)(entries: MapWriter => Unit): Unit

  def writeBoolean(schema: Schema, value: Boolean): Unit
  def writeByte(schema: Schema, value: Byte): Unit
  def writeShort(schema: Schema, value: Short): Unit
  def writeInteger(schema: Schema, value: Int): Unit
  def writeLong(schema: Schema, value: Long): Unit
  def writeFloat(schema: Schema, value: Float): Unit
  def writeDouble(schema: Schema, value: Double): Unit
  def writeBigInteger(schema: Schema, value: BigInt): Unit
  def writeBigDecimal(schema: Schema, value: BigDecimal): Unit
  def writeString(schema: Schema, value: String): Unit
  def writeBlob(schema: Schema, value: Blob): Unit
  def writeTimestamp(schema: Schema, value: Instant): Unit
  def writeDocument(schema: Schema, value: Document): Unit
  def writeNull(schema: Schema): Unit
}

trait MapWriter {
  def writeEntry(key: String)(value: ShapeWriter => Unit): Unit
}

/** The reading side: a codec positions a reader on one value at a time. */
trait ShapeReader {

  /** True when the current value is absent or null. */
  def isNull: Boolean

  /**
    * Calls `onMember` for every member of `schema` present in the input. Names that aren't members
    * go to `onUnknown` (unions use it to record unknown variants); everything else is skipped.
    */
  def readStruct(schema: Schema, onUnknown: String => Unit = _ => ())(
      onMember: (Schema, ShapeReader) => Unit
  ): Unit
  def readList(schema: Schema)(onElement: ShapeReader => Unit): Unit
  def readMap(schema: Schema)(onEntry: (String, ShapeReader) => Unit): Unit

  def readBoolean(schema: Schema): Boolean
  def readByte(schema: Schema): Byte
  def readShort(schema: Schema): Short
  def readInteger(schema: Schema): Int
  def readLong(schema: Schema): Long
  def readFloat(schema: Schema): Float
  def readDouble(schema: Schema): Double
  def readBigInteger(schema: Schema): BigInt
  def readBigDecimal(schema: Schema): BigDecimal
  def readString(schema: Schema): String
  def readBlob(schema: Schema): Blob
  def readTimestamp(schema: Schema): Instant
  def readDocument(schema: Schema): Document
}

final class SerializationException(message: String, cause: Throwable = null)
    extends RuntimeException(message, cause)
