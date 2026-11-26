package smithyscala.runtime
package codecs

/**
  * A format codec: turns what a shape codec writes ([[ShapeWriter]] calls) into bytes, and bytes back into
  * [[ShapeReader]] calls. It knows one format and nothing about HTTP; a protocol picks the codec and its settings.
  *
  * The formats Smithy protocols use, and their state here:
  *
  * | Codec            | Format                     | Used by                                         | State           |
  * |------------------|----------------------------|-------------------------------------------------|-----------------|
  * | [[JsonCodec]]    | JSON                       | awsJson1_0/1_1, rpcv2Json (restJson1 would too) | implemented     |
  * | [[XmlCodec]]     | XML                        | restXml; awsQuery and ec2Query responses        | not implemented |
  * | [[FormUrlCodec]] | `application/x-www-form-urlencoded` | awsQuery and ec2Query requests         | not implemented |
  * | [[CborCodec]]    | CBOR (RFC 8949)            | rpcv2Cbor                                       | not implemented |
  * | [[ProtobufCodec]]| Protocol Buffers           | the gRPC design in docs/smithy/new-protocol-protobuf.md | not implemented |
  *
  * The unimplemented ones describe what they would read from the schema and fail with
  * [[CodecNotImplemented]] if used.
  */
trait Codec {

  /** The media type of what this codec writes, e.g. `application/json`. */
  def mediaType: String

  /** Runs `write` against a writer for this format and returns the bytes. */
  def serialize(write: ShapeWriter => Unit): Array[Byte]

  /** Reads `bytes` with `read`, given a reader positioned on the whole document. */
  def deserialize[A](bytes: Array[Byte])(read: ShapeReader => A): A
}

/** A codec that exists as a placeholder: its format isn't implemented yet. */
final class CodecNotImplemented(codec: String, mediaType: String)
    extends UnsupportedOperationException(s"$codec ($mediaType) is not implemented yet")

/** Base for the placeholders: every operation fails with [[CodecNotImplemented]]. */
abstract class PlannedCodec(name: String) extends Codec {
  final def serialize(write: ShapeWriter => Unit): Array[Byte]       = throw new CodecNotImplemented(name, mediaType)
  final def deserialize[A](bytes: Array[Byte])(read: ShapeReader => A): A = throw new CodecNotImplemented(name, mediaType)
}
