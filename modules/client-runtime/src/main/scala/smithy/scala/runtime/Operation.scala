package smithy.scala.runtime

import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.compiletime.summonInline

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

object TypeRegistry {

  /**
    * A registry from a tuple of error types, `TypeRegistry.of[A *: B *: EmptyTuple]`. The tuple is
    * walked at compile time: each type's codec is summoned, and a type that isn't a
    * `ServiceException` is a compile error rather than a runtime surprise.
    */
  inline def of[Es <: Tuple]: TypeRegistry = new TypeRegistry(errorTypes[Es])

  private inline def errorTypes[Es <: Tuple]: List[ErrorType] = inline erasedValue[Es] match {
    case _: EmptyTuple => Nil
    case _: (e *: es) =>
      inline erasedValue[e] match {
        case _: ServiceException =>
          val codec = summonInline[SchemaCodec[e]]
          ErrorType(codec.schema, r => codec.read(r, codec.schema).asInstanceOf[ServiceException]) :: errorTypes[es]
        case _ => error("TypeRegistry.of: every error type must extend ServiceException")
      }
  }
}

/** Everything the pipeline needs to call one operation. Generated once per operation. */
final class Operation[I, O](
    val id: ShapeId,
    input: SchemaCodec[I],
    output: SchemaCodec[O],
    val errors: TypeRegistry,
    val authSchemes: List[String]
) {
  def name: String                                = id.name
  def inputSchema: Schema                         = input.schema
  def writeInput(w: ShapeWriter, value: I): Unit  = input.write(w, input.schema, value)
  def readOutput(r: ShapeReader): O               = output.read(r, output.schema)
}

object Operation {

  /** The input and output codecs come from context bounds, named with `as`. */
  def of[I: SchemaCodec as in, O: SchemaCodec as out](id: ShapeId, errors: TypeRegistry, authSchemes: List[String]): Operation[I, O] =
    new Operation(id, in, out, errors, authSchemes)
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
