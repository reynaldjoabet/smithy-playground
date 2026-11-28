package smithy.scala.codegen.jsonrpc

import java.util.List as JList

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{OperationShape, ServiceShape, Shape, ShapeId}
import software.amazon.smithy.model.validation.{AbstractValidator, ValidationEvent}

/**
  * Validators for the JSON-RPC protocol (resources/META-INF/smithy/jsonrpc.smithy), ported from
  * jsonrpclib's `jsonrpclib.validation` (Apache License 2.0) so this repo doesn't depend on jsonrpclib.
  * The event ids and messages are the same as jsonrpclib's. They're registered in
  * resources/META-INF/services/software.amazon.smithy.model.validation.Validator, so every model loaded
  * with this module on the classpath is checked.
  *
  * They run wherever this module is on the classpath (sbt, smithy-build with the plugin), but not in the
  * VS Code Smithy language server, which only sees the jars listed in smithy-build.json.
  */
private[jsonrpc] object JsonRpc {
  val Protocol: ShapeId     = ShapeId.from("jsonrpc#jsonRpc")
  val Request: ShapeId      = ShapeId.from("jsonrpc#jsonRpcRequest")
  val Notification: ShapeId = ShapeId.from("jsonrpc#jsonRpcNotification")

  def services(model: Model): List[ServiceShape] =
    model.getServiceShapes.asScala.filter(_.hasTrait(Protocol)).toList.sortBy(_.getId.toString)

  def operations(model: Model, service: ServiceShape): List[OperationShape] =
    service.getAllOperations.asScala.toList.map(id => model.expectShape(id, classOf[OperationShape]))

  /** The method name from `@jsonRpcRequest`, or else from `@jsonRpcNotification`. */
  def methodName(op: Shape): Option[String] =
    (op.findTrait(Request).toScala orElse op.findTrait(Notification).toScala).map(_.toNode.expectStringNode.getValue)
}

/** An operation marked `@jsonRpcNotification` is fire-and-forget, so it must have no output. */
final class JsonNotificationOutputValidator extends AbstractValidator {
  override def validate(model: Model): JList[ValidationEvent] =
    model.getShapesWithTrait(JsonRpc.Notification).asScala.toList.sortBy(_.getId.toString).flatMap { op =>
      val outputId = op.asOperationShape.orElseThrow().getOutputShape
      val hasOutput = model.expectShape(outputId).asStructureShape.toScala.forall(!_.members.isEmpty)
      Option.when(hasOutput)(
        error(op, s"Operation marked as @jsonRpcNotification must not return anything, but found `$outputId`.")
      )
    }.asJava
}

/** Every operation of a `@jsonRpc` service needs a method name: `@jsonRpcRequest` or `@jsonRpcNotification`. */
final class JsonRpcOperationValidator extends AbstractValidator {
  override def validate(model: Model): JList[ValidationEvent] =
    JsonRpc.services(model).flatMap { service =>
      JsonRpc.operations(model, service).filter(JsonRpc.methodName(_).isEmpty).map { op =>
        error(
          op,
          s"Operation is part of service `${service.getId}` marked with @jsonRpc but is missing @jsonRpcRequest or @jsonRpcNotification."
        )
      }
    }.asJava
}

/** Within one `@jsonRpc` service, each method name belongs to one operation. */
final class UniqueJsonRpcMethodNamesValidator extends AbstractValidator {
  override def validate(model: Model): JList[ValidationEvent] =
    JsonRpc.services(model).flatMap { service =>
      val byName = JsonRpc.operations(model, service).flatMap(op => JsonRpc.methodName(op).map(_ -> op)).groupMap(_._1)(_._2)
      byName.toList.sortBy(_._1).collect {
        case (name, ops) if ops.size > 1 =>
          val users = ops.map(_.getId.toString).sorted.mkString(", ")
          ops.map(_ => error(service, s"Duplicate JSON-RPC method name `$name` in service `${service.getId}`. It is used by: $users"))
      }.flatten
    }.asJava
}
