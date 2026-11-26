package smithyscala.runtime
package codecs

/**
  * How a protocol wants XML written.
  *
  * @param defaultNamespace
  *   the service's `@xmlNamespace`, put on the root element when the shape has none of its own
  * @param wrapInResult
  *   awsQuery wraps output in `<OperationResponse><OperationResult>`; ec2Query and restXml don't
  */
final case class XmlSettings(defaultNamespace: Option[String] = None, wrapInResult: Boolean = false)

/**
  * Not implemented: XML, for restXml (S3, Route 53, CloudFront) and the responses of awsQuery and ec2Query.
  *
  * What it would read from the schema:
  *   - `@xmlName`: the element (or attribute) name instead of the member name;
  *   - `@xmlAttribute`: write the member as an attribute of the enclosing element;
  *   - `@xmlFlattened`: a list or map without a wrapping element, one element per entry;
  *   - `@xmlNamespace`: an `xmlns` (and prefix) on the element;
  *   - `@timestampFormat`: XML's default is `date-time`, not epoch seconds.
  *
  * Lists are `<member>` elements and maps `<entry><key/><value/></entry>` unless renamed or flattened; blobs are base64.
  * The AWS Emulator pages in `docs/aws-emulator` show real restXml and awsQuery bodies.
  */
final class XmlCodec(val settings: XmlSettings = XmlSettings()) extends PlannedCodec("XmlCodec") {
  val mediaType: String = "application/xml"
}
