package smithy.scala.codegen.jsonrpc

import java.util.List as JList

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.validation.{AbstractValidator, ValidationEvent}

/** Validates that operations marked with `@jsonRpcNotification` don't have any output. */
final class JsonNotificationOutputValidator extends AbstractValidator {

  override def validate(model: Model): JList[ValidationEvent] =
    model.getShapesWithTrait(JsonRpcTraits.JsonRpcNotification).asScala.toList.sortBy(_.getId.toString).flatMap { op =>
      val outputShapeId = op.asOperationShape.orElseThrow().getOutputShape
      val outputShape   = model.expectShape(outputShapeId)
      // As in jsonrpclib: an output that isn't a structure, or is one with members, is an error.
      if (outputShape.asStructureShape.toScala.forall(!_.members.isEmpty))
        List(error(op, s"Operation marked as @jsonRpcNotification must not return anything, but found `$outputShapeId`."))
      else Nil
    }.asJava
}
