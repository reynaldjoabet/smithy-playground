package smithyscala.runtime
package codecs

import zio.blocks.schema.{Schema as ZSchema}
import zio.blocks.schema.DynamicValue
import zio.blocks.schema.SchemaError
import zio.blocks.schema.avro.AvroFormat
import zio.blocks.schema.json.JsonFormat
import zio.blocks.schema.msgpack.MessagePackFormat
import zio.blocks.schema.thrift.ThriftFormat
import zio.blocks.schema.yaml.YamlFormat

/**
  * A codec whose bytes come from a ZIO Blocks format. Our shape codecs still decide what is written
  * (member names, which members, union cases); the [[DynamicValueWriter]] collects those calls into a
  * ZIO Blocks `DynamicValue`, and the format's `DynamicValue` codec writes the bytes. Reading runs the
  * other way through a [[DynamicValueReader]].
  *
  * No Smithy protocol uses these formats. They are here so the same generated shapes can be written
  * as JSON (ZIO Blocks' own), MessagePack, Avro, YAML or Thrift without new derivation code. Smithy's JSON stays on
  * [[JsonCodec]], which follows Smithy's JSON rules (`@jsonName`, `@timestampFormat`, non-finite
  * numbers as strings) that ZIO Blocks' JSON doesn't know about.
  */
abstract class DynamicValueCodec(name: String) extends Codec {

  protected def encode(value: DynamicValue): Array[Byte]
  protected def decode(bytes: Array[Byte]): Either[SchemaError, DynamicValue]

  /** Runs `write` and returns what it wrote as a `DynamicValue`, before any format is involved. */
  final def toDynamicValue(write: ShapeWriter => Unit): DynamicValue = {
    var result: DynamicValue = DynamicValue.Null
    write(new DynamicValueWriter((_, value) => result = value))
    result
  }

  /** Reads a `DynamicValue` with `read`, as [[deserialize]] does after decoding. */
  final def fromDynamicValue[A](value: DynamicValue)(read: ShapeReader => A): A = read(new DynamicValueReader(value))

  final def serialize(write: ShapeWriter => Unit): Array[Byte] = encode(toDynamicValue(write))

  final def deserialize[A](bytes: Array[Byte])(read: ShapeReader => A): A = {
    val value =
      try decode(bytes)
      catch { case e: RuntimeException => throw new SerializationException(s"Invalid $name body: ${e.getMessage}", e) }
    value match {
      case Right(v) => fromDynamicValue(v)(read)
      case Left(e)  => throw new SerializationException(s"Invalid $name body: ${e.message}", e)
    }
  }
}

/** The `DynamicValue` codec of each ZIO Blocks format, derived once. */
private object ZioBlocksFormats {
  lazy val messagePack = ZSchema[DynamicValue].derive(MessagePackFormat)
  lazy val avro        = ZSchema[DynamicValue].derive(AvroFormat)
  lazy val yaml        = ZSchema[DynamicValue].derive(YamlFormat)
  lazy val json        = ZSchema[DynamicValue].derive(JsonFormat)
  lazy val thrift      = ZSchema[DynamicValue].derive(ThriftFormat)
}

/** MessagePack (`application/msgpack`), through `zio-blocks-schema-messagepack`. Round-trips. */
final class MessagePackCodec extends DynamicValueCodec("MessagePack") {
  val mediaType: String = MessagePackFormat.mimeType
  protected def encode(value: DynamicValue): Array[Byte]                      = ZioBlocksFormats.messagePack.encode(value)
  protected def decode(bytes: Array[Byte]): Either[SchemaError, DynamicValue] = ZioBlocksFormats.messagePack.decode(bytes)
}

/**
  * Avro (`application/avro`), through `zio-blocks-schema-avro`. Round-trips. Avro needs a writer schema;
  * ZIO Blocks uses its fixed, recursive Avro schema for `DynamicValue`, not one made from the Smithy shape.
  */
final class AvroCodec extends DynamicValueCodec("Avro") {
  val mediaType: String = AvroFormat.mimeType
  protected def encode(value: DynamicValue): Array[Byte]                      = ZioBlocksFormats.avro.encode(value)
  protected def decode(bytes: Array[Byte]): Either[SchemaError, DynamicValue] = ZioBlocksFormats.avro.decode(bytes)
}

/** YAML (`application/yaml`), through `zio-blocks-schema-yaml`. Round-trips; unions come back as one-entry maps. */
final class YamlCodec extends DynamicValueCodec("YAML") {
  val mediaType: String = YamlFormat.mimeType
  protected def encode(value: DynamicValue): Array[Byte]                      = ZioBlocksFormats.yaml.encode(value)
  protected def decode(bytes: Array[Byte]): Either[SchemaError, DynamicValue] = ZioBlocksFormats.yaml.decode(bytes)
}

/**
  * JSON (`application/json`) written by ZIO Blocks' JSON codec in `zio-blocks-schema`, not by Jackson.
  * Unlike [[JsonCodec]] it knows no Smithy JSON rules: no `@jsonName`, no `@timestampFormat` (timestamps
  * are ISO-8601 strings, see [[DynamicValueWriter]]), and non-finite doubles aren't written as `"NaN"`.
  * Use [[JsonCodec]] for the Smithy protocols; this one is the plain-JSON member of the bridge.
  */
final class ZioBlocksJsonCodec extends DynamicValueCodec("JSON") {
  val mediaType: String = JsonFormat.mimeType
  protected def encode(value: DynamicValue): Array[Byte]                      = ZioBlocksFormats.json.encode(value)
  protected def decode(bytes: Array[Byte]): Either[SchemaError, DynamicValue] = ZioBlocksFormats.json.decode(bytes)
}

/**
  * Thrift (`application/thrift`), through `zio-blocks-schema-thrift`. Write-only: ZIO Blocks' Thrift codec
  * can't decode a `DynamicValue` without a schema, so [[deserialize]] fails with a [[SerializationException]].
  */
final class ThriftCodec extends DynamicValueCodec("Thrift") {
  val mediaType: String = ThriftFormat.mimeType
  protected def encode(value: DynamicValue): Array[Byte]                      = ZioBlocksFormats.thrift.encode(value)
  protected def decode(bytes: Array[Byte]): Either[SchemaError, DynamicValue] = ZioBlocksFormats.thrift.decode(bytes)
}
