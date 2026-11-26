package smithyscala.runtime

/** Details of a modelled error a response turned into, filled in by the protocol. */
final case class ErrorMetadata(code: String, statusCode: Int, requestId: Option[String], headers: Headers)

/**
  * The base class of every generated error. Accessors are prefixed `smithy` so they can't clash
  * with an error's own members (AWS errors often have `Code` or `ErrorCode` members).
  */
abstract class ServiceException(val smithyShapeId: ShapeId, message: String, val smithyFault: String)
    extends RuntimeException(if (message eq null) smithyShapeId.name else message) {
  @volatile private var metadata: Option[ErrorMetadata] = None
  def smithyMetadata: Option[ErrorMetadata]             = metadata
  private[runtime] def attach(m: ErrorMetadata): Unit   = metadata = Some(m)

  /** True when the model marks the error `@retryable`. */
  def smithyRetryable: Boolean  = false
  def smithyThrottling: Boolean = false
}

/** An error code the operation doesn't model. */
final class UnknownServiceException(val code: String, message: String, statusCode: Int)
    extends ServiceException(ShapeId("", code), s"$code (HTTP $statusCode): $message", if (statusCode >= 500) "server" else "client")

/** How to build one modelled error from a response body. */
final class ErrorType(val schema: Schema, val read: ShapeReader => ServiceException)

/** Maps error codes from the wire to generated error classes (smithy-java's `TypeRegistry`). */
final class TypeRegistry(types: Seq[ErrorType]) {
  private val byId   = types.map(t => t.schema.id -> t).toMap
  private val byName = types.map(t => t.schema.id.name -> t).toMap

  /** Accepts a full shape id or a bare name, as protocols send either. */
  def find(code: String): Option[ErrorType] =
    if (code.contains('#')) byId.get(ShapeId.parse(code)).orElse(byName.get(ShapeId.parse(code).name))
    else byName.get(code)
}

/** Everything the pipeline needs to call one operation. Generated once per operation. */
final class Operation[I, O](
    val id: ShapeId,
    val inputSchema: Schema,
    val writeInput: (ShapeWriter, I) => Unit,
    val outputSchema: Schema,
    val readOutput: ShapeReader => O,
    val errors: TypeRegistry,
    val authSchemes: List[String]
) {
  def name: String = id.name
}

enum ProtocolInfo derives CanEqual {

  /** `aws.protocols#awsJson1_0` or `awsJson1_1`. */
  case AwsJson(version: String)

  /** `smithy.protocols#rpcv2Json`. */
  case RpcV2Json
}

/** Service-level facts from the model that the runtime needs. */
final case class ServiceInfo(
    id: ShapeId,
    sdkId: String,
    endpointPrefix: Option[String],
    signingName: Option[String],
    protocol: ProtocolInfo
)
