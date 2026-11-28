package smithy.scala.codegen

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{ListShape, MapShape, MemberShape, Shape}
import software.amazon.smithy.model.shapes.ShapeId as SmithyId

import smithy.scala.runtime as rt

/**
  * Builds the runtime's [[rt.Schema]] for a shape straight from a Smithy model, at build time, with no
  * generated code. It's the same schema the generator writes into each companion (`X.schema`), so
  * anything computed from a runtime schema, such as [[rt.codecs.AvroSchemas]], gives the same answer
  * from the model as from generated code.
  *
  * Only the traits that change a schema's encoding are carried: `@required`, `@sparse`, `@jsonName`
  * and `@timestampFormat`.
  */
object ModelSchemas {

  private val carried = List(rt.Traits.Required, rt.Traits.Sparse, rt.Traits.JsonName, rt.Traits.TimestampFormat)

  def of(model: Model, id: SmithyId): rt.Schema = new Builder(model).schema(id)

  private final class Builder(model: Model) {
    private val built = mutable.Map.empty[SmithyId, rt.Schema]

    // Members refer to their targets through thunks, so a recursive shape finds itself in `built`.
    def schema(id: SmithyId): rt.Schema = built.getOrElseUpdate(id, build(model.expectShape(id)))

    private def build(shape: Shape): rt.Schema = {
      val id     = rt.ShapeId(shape.getId.getNamespace, shape.getId.getName)
      val traits = traitsOf(shape)
      def members(ms: Seq[MemberShape]): Seq[rt.MemberDef] =
        ms.map(m => rt.MemberDef(m.getMemberName, () => schema(m.getTarget), traitsOf(m)))
      shape.getType.name match {
        case "STRUCTURE" => rt.Schema.aggregate(id, rt.ShapeType.Structure, traits)(members(shape.members.asScala.toSeq)*)
        case "UNION"     => rt.Schema.aggregate(id, rt.ShapeType.Union, traits)(members(shape.members.asScala.toSeq)*)
        case "LIST" | "SET" =>
          rt.Schema.aggregate(id, rt.ShapeType.List, traits)(members(Seq(shape.asInstanceOf[ListShape].getMember))*)
        case "MAP" =>
          val map = shape.asInstanceOf[MapShape]
          rt.Schema.aggregate(id, rt.ShapeType.Map, traits)(members(Seq(map.getKey, map.getValue))*)
        case other => rt.Schema.simple(id, simpleType(shape, other), traits)
      }
    }

    // Compared by name: Java enums have no CanEqual, and the build uses -language:strictEquality.
    private def simpleType(shape: Shape, t: String): rt.ShapeType = t match {
      case "BOOLEAN"     => rt.ShapeType.Boolean
      case "BYTE"        => rt.ShapeType.Byte
      case "SHORT"       => rt.ShapeType.Short
      case "INTEGER"     => rt.ShapeType.Integer
      case "LONG"        => rt.ShapeType.Long
      case "FLOAT"       => rt.ShapeType.Float
      case "DOUBLE"      => rt.ShapeType.Double
      case "BIG_INTEGER" => rt.ShapeType.BigInteger
      case "BIG_DECIMAL" => rt.ShapeType.BigDecimal
      case "STRING"      => rt.ShapeType.String
      case "ENUM"        => rt.ShapeType.Enum
      case "INT_ENUM"    => rt.ShapeType.IntEnum
      case "BLOB"        => rt.ShapeType.Blob
      case "TIMESTAMP"   => rt.ShapeType.Timestamp
      case "DOCUMENT"    => rt.ShapeType.Document
      case _ => throw new IllegalArgumentException(s"${shape.getId} is a $t, which has no data schema")
    }

    private def traitsOf(shape: Shape): rt.Traits =
      rt.Traits(carried.flatMap { traitId =>
        shape.findTrait(traitId).toScala.map(t => traitId -> t.toNode.asStringNode.toScala.fold("")(_.getValue))
      }*)
  }
}
