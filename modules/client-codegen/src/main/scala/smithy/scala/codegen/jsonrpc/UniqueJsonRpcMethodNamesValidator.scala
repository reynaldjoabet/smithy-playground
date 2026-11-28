package smithy.scala.codegen.jsonrpc

import java.util.List as JList

import scala.jdk.CollectionConverters.*

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.validation.{AbstractValidator, ValidationEvent}

/** Validates that no two operations of a `@jsonRpc` service share a JSON-RPC method name. */
final class UniqueJsonRpcMethodNamesValidator extends AbstractValidator {

  override def validate(model: Model): JList[ValidationEvent] =
    JsonRpcTraits.services(model).flatMap(validateService(model, _)).asJava

  private def validateService(model: Model, service: ServiceShape): List[ValidationEvent] = {
    val methodsToOps = JsonRpcTraits.operations(model, service)
      .flatMap(op => JsonRpcTraits.methodName(op).map(_ -> op))
      .groupMap(_._1)(_._2)
    // One event per operation that uses a duplicated name, as in jsonrpclib.
    methodsToOps.toList.sortBy(_._1).flatMap {
      case (name, ops) if ops.size > 1 =>
        val users = ops.map(_.getId.toString).sorted.mkString(", ")
        ops.map(_ => error(service, s"Duplicate JSON-RPC method name `$name` in service `${service.getId}`. It is used by: $users"))
      case _ => Nil
    }
  }
}
