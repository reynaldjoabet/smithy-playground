package smithy.scala.codegen.jsonrpc

import java.util.List as JList

import scala.jdk.CollectionConverters.*

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{ServiceShape, Shape}
import software.amazon.smithy.model.validation.{AbstractValidator, ValidationEvent}

/** Validates that every operation of a `@jsonRpc` service has `@jsonRpcRequest` or `@jsonRpcNotification`. */
final class JsonRpcOperationValidator extends AbstractValidator {

  override def validate(model: Model): JList[ValidationEvent] =
    JsonRpcTraits.services(model).flatMap(validateService(model, _)).asJava

  private def validateService(model: Model, service: ServiceShape): List[ValidationEvent] =
    JsonRpcTraits.operations(model, service).filterNot(hasJsonRpcMethod).map { op =>
      error(
        op,
        s"Operation is part of service `${service.getId}` marked with @jsonRpc but is missing @jsonRpcRequest or @jsonRpcNotification."
      )
    }

  private def hasJsonRpcMethod(op: Shape): Boolean =
    op.hasTrait(JsonRpcTraits.JsonRpcRequest) || op.hasTrait(JsonRpcTraits.JsonRpcNotification)
}
