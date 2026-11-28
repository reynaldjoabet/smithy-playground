package smithy.scala.codegen.jsonrpc

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{OperationShape, ServiceShape, Shape, ShapeId}

/**
  * The JSON-RPC protocol's traits (resources/META-INF/smithy/jsonrpc.smithy), by shape id. jsonrpclib has a
  * Java class per trait (`JsonRpcTrait`, `JsonRpcRequestTrait`, `JsonRpcNotificationTrait`); the validators
  * here only need the ids and the method-name string, so there are no trait classes.
  *
  * The validators in this package are ported from jsonrpclib's `jsonrpclib.validation` (Apache License
  * 2.0), one file per validator as there, with the same event ids and messages. They're registered in
  * resources/META-INF/services/software.amazon.smithy.model.validation.Validator. They run wherever this
  * module is on the classpath (sbt, smithy-build with the plugin), but not in the VS Code Smithy language
  * server, which only sees the jars listed in smithy-build.json.
  */
object JsonRpcTraits {
  val JsonRpc: ShapeId             = ShapeId.from("jsonrpc#jsonRpc")
  val JsonRpcRequest: ShapeId      = ShapeId.from("jsonrpc#jsonRpcRequest")
  val JsonRpcNotification: ShapeId = ShapeId.from("jsonrpc#jsonRpcNotification")

  /** Services marked `@jsonRpc`. */
  def services(model: Model): List[ServiceShape] =
    model.getServiceShapes.asScala.filter(_.hasTrait(JsonRpc)).toList.sortBy(_.getId.toString)

  /** A service's operations, including those bound through resources. */
  def operations(model: Model, service: ServiceShape): List[OperationShape] =
    service.getAllOperations.asScala.toList.map(id => model.expectShape(id, classOf[OperationShape]))

  /** The method name from `@jsonRpcRequest`, or else from `@jsonRpcNotification`. */
  def methodName(op: Shape): Option[String] =
    (op.findTrait(JsonRpcRequest).toScala orElse op.findTrait(JsonRpcNotification).toScala)
      .map(_.toNode.expectStringNode.getValue)
}
