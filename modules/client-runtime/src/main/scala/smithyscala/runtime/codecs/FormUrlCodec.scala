package smithyscala.runtime
package codecs

/**
  * How a protocol wants form parameters named.
  *
  * @param ec2Names
  *   ec2Query's naming: `@ec2QueryName`, else the capitalized `@xmlName`, else the capitalized member name; lists are
  *   always flattened (`TagSpecification.1.Tag.1.Key`)
  */
final case class FormUrlSettings(ec2Names: Boolean = false)

/**
  * Not implemented: `application/x-www-form-urlencoded` request bodies, for awsQuery (IAM, STS, SNS, RDS ...) and
  * ec2Query (EC2). Responses of those protocols are XML ([[XmlCodec]]).
  *
  * The protocol adds `Action=<Operation>&Version=<service version>`; this codec writes the input's members:
  *   - nested members join with dots: `Attributes.entry.1.key=VisibilityTimeout`;
  *   - lists are `Name.member.1=...`, or `Name.1=...` when `@xmlFlattened` (and always for ec2Query);
  *   - maps are `Name.entry.1.key` / `Name.entry.1.value`;
  *   - `@xmlName` (and `@ec2QueryName`) rename keys; timestamps are `date-time`; blobs are base64;
  *   - an empty list is written as `Name=` so the server sees it was set.
  *
  * Writing only: query protocols never send form-encoded responses, so `deserialize` has nothing to read.
  */
final class FormUrlCodec(val settings: FormUrlSettings = FormUrlSettings()) extends PlannedCodec("FormUrlCodec") {
  val mediaType: String = "application/x-www-form-urlencoded; charset=utf-8"
}
