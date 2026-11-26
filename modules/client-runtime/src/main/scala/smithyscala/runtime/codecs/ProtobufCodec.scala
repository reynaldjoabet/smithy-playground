package smithyscala.runtime
package codecs

/**
  * Not implemented: Protocol Buffers, for the gRPC protocol designed in `docs/smithy/new-protocol-protobuf.md`. No AWS
  * service uses it; it shows a codec whose format identifies fields by number instead of name.
  *
  * What it would read from the schema:
  *   - `smithy.protocols#idx` (Smithy 1.75+): the member's field number; a service marked `@indexed` has one on every
  *     member. This codec cannot work without it;
  *   - the shape type, to pick the wire type: varint (booleans, integers, intEnums), 32-bit (float), 64-bit (double),
  *     length-delimited (strings, enums, blobs, nested structures, packed numeric lists).
  *
  * Reading has to skip unknown field numbers by wire type (forward compatibility) and merge repeated fields that
  * arrive interleaved. Documents and `@sparse` collections have no Protobuf form.
  */
final class ProtobufCodec extends PlannedCodec("ProtobufCodec") {
  val mediaType: String = "application/x-protobuf"
}
