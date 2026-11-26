package smithyscala.runtime

import java.net.URI

import com.fasterxml.jackson.databind.JsonNode

/**
  * A protocol turns an operation call into an HTTP request, and an HTTP response into the output or
  * an error. It owns method, path, headers and error envelopes, and calls a codec for the body.
  */
trait ClientProtocol {
  def id: String
  def createRequest[I, O](operation: Operation[I, O], input: I, endpoint: URI): HttpRequest
  def deserializeResponse[I, O](operation: Operation[I, O], response: HttpResponse): O
}

object ClientProtocol {
  def forService(service: ServiceInfo): ClientProtocol = service.protocol match {
    case ProtocolInfo.AwsJson(version) => new AwsJsonProtocol(service, version)
    case ProtocolInfo.RpcV2Json        => new RpcV2JsonProtocol(service)
  }
}

/** What the RPC-over-JSON protocols share: a JSON body, and an error code to look up. */
abstract class JsonRpcProtocolBase(service: ServiceInfo, settings: JsonSettings) extends ClientProtocol {
  protected val codec = new JsonCodec(settings)

  protected def body[I, O](operation: Operation[I, O], input: I): Array[Byte] =
    codec.serialize(w => operation.writeInput(w, input))

  protected def withPath(endpoint: URI, path: String): URI = {
    val base = Option(endpoint.getRawPath).getOrElse("").stripSuffix("/")
    endpoint.resolve(base + path)
  }

  /** Where this protocol puts the error code. */
  protected def errorCode(response: HttpResponse, body: JsonNode): Option[String]

  def deserializeResponse[I, O](operation: Operation[I, O], response: HttpResponse): O = {
    val tree = codec.readTree(response.body)
    if (response.status >= 200 && response.status < 300) operation.readOutput(codec.reader(tree))
    else throw toError(operation, response, tree)
  }

  private def toError[I, O](operation: Operation[I, O], response: HttpResponse, tree: JsonNode): ServiceException = {
    val raw  = errorCode(response, tree).getOrElse(s"Http${response.status}")
    // Codes may carry extra data after ':' (awsJson), and may or may not include the namespace.
    val code = raw.takeWhile(_ != ':')
    val requestId = response.headers.get("x-amzn-RequestId").orElse(response.headers.get("x-amz-request-id"))
    val error = operation.errors.find(code) match {
      case Some(t) => t.read(codec.reader(tree))
      case None =>
        val message = Seq("message", "Message").map(tree.path).find(_.isTextual).map(_.textValue).getOrElse("")
        new UnknownServiceException(ShapeId.parse(code).name, message, response.status)
    }
    error.attach(ErrorMetadata(ShapeId.parse(code).name, response.status, requestId, response.headers))
    error
  }

  protected def text(node: JsonNode, field: String): Option[String] =
    Option(node.get(field)).filter(_.isTextual).map(_.textValue)
}

/**
  * `aws.protocols#awsJson1_0` / `awsJson1_1`: `POST /`, the operation in `X-Amz-Target`, the input as
  * the JSON body. `@jsonName` is ignored, `@timestampFormat` is honoured.
  */
final class AwsJsonProtocol(service: ServiceInfo, version: String)
    extends JsonRpcProtocolBase(service, JsonSettings(useJsonName = false, useTimestampFormat = true)) {

  val id: String = if (version == "1.0") "aws.protocols#awsJson1_0" else "aws.protocols#awsJson1_1"

  def createRequest[I, O](operation: Operation[I, O], input: I, endpoint: URI): HttpRequest =
    HttpRequest(
      "POST",
      withPath(endpoint, "/"),
      Headers(
        "Content-Type" -> s"application/x-amz-json-$version",
        "X-Amz-Target" -> s"${service.id.name}.${operation.name}"
      ),
      body(operation, input)
    )

  protected def errorCode(response: HttpResponse, body: JsonNode): Option[String] =
    response.headers.get("X-Amzn-Errortype").orElse(text(body, "__type")).orElse(text(body, "code"))
}

/**
  * `smithy.protocols#rpcv2Json`: `POST /service/<Service>/operation/<Operation>` with a
  * `Smithy-Protocol: rpc-v2-json` header. Errors name their shape in `__type`.
  */
final class RpcV2JsonProtocol(service: ServiceInfo)
    extends JsonRpcProtocolBase(service, JsonSettings(useJsonName = false, useTimestampFormat = false)) {

  val id: String = "smithy.protocols#rpcv2Json"

  def createRequest[I, O](operation: Operation[I, O], input: I, endpoint: URI): HttpRequest = {
    val unitInput = operation.inputSchema.id == Prelude.Unit.id
    val headers =
      if (unitInput) Headers("Smithy-Protocol" -> "rpc-v2-json", "Accept" -> "application/json")
      else
        Headers(
          "Smithy-Protocol" -> "rpc-v2-json",
          "Accept"          -> "application/json",
          "Content-Type"    -> "application/json"
        )
    HttpRequest(
      "POST",
      withPath(endpoint, s"/service/${service.id.name}/operation/${operation.name}"),
      headers,
      if (unitInput) Array.emptyByteArray else body(operation, input)
    )
  }

  protected def errorCode(response: HttpResponse, body: JsonNode): Option[String] =
    text(body, "__type")
}
