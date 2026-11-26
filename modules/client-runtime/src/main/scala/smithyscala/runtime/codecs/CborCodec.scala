package smithyscala.runtime
package codecs

/**
  * Not implemented: CBOR ([RFC 8949](https://www.rfc-editor.org/rfc/rfc8949)), for `smithy.protocols#rpcv2Cbor`.
  *
  * A binary JSON-like format, so it walks shapes the way [[JsonCodec]] does, with these differences:
  *   - member names are always the Smithy member names (`@jsonName` does not apply);
  *   - timestamps are tag 1 (epoch seconds) with a floating-point or integer value;
  *   - blobs are byte strings, not base64 text;
  *   - `bigInteger` and `bigDecimal` use tags 2/3 and 4;
  *   - `NaN` and infinities are native floating-point values, not strings;
  *   - documents are not supported by rpcv2Cbor.
  *
  * smithy-java's `codecs:cbor-codec` and smithy-go's `encoding/cbor` are working implementations to follow.
  */
final class CborCodec extends PlannedCodec("CborCodec") {
  val mediaType: String = "application/cbor"
}
