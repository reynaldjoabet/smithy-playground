package smithyscala.runtime

import java.nio.charset.StandardCharsets

/** An absolute shape id, such as `com.amazonaws.sqs#SendMessage`. */
final case class ShapeId(namespace: String, name: String) derives CanEqual {
  override def toString: String = s"$namespace#$name"
}

object ShapeId {
  def parse(id: String): ShapeId = {
    val hash = id.indexOf('#')
    if (hash < 0) ShapeId("", id) else ShapeId(id.substring(0, hash), id.substring(hash + 1))
  }
}

enum ShapeType derives CanEqual {
  case Boolean, Byte, Short, Integer, Long, Float, Double, BigInteger, BigDecimal
  case String, Enum, IntEnum, Blob, Timestamp, Document
  case List, Map, Structure, Union
}

/**
  * The traits a schema carries at runtime, keyed by absolute trait id. Only the traits that a
  * codec, protocol or the pipeline reads are kept (the "runtime traits"); values are the trait's
  * simple value as a string, or "" for annotation traits such as `@required`.
  */
final case class Traits(values: Map[String, String]) {
  def has(id: String): Boolean             = values.contains(id)
  def get(id: String): Option[String]      = values.get(id)
  def ++(other: Traits): Traits            = Traits(values ++ other.values)
  override def toString: String            = values.mkString("Traits(", ", ", ")")
}

object Traits {
  val empty: Traits                        = Traits(Map.empty)
  def apply(pairs: (String, String)*): Traits = Traits(pairs.toMap)

  val Required         = "smithy.api#required"
  val JsonName         = "smithy.api#jsonName"
  val TimestampFormat  = "smithy.api#timestampFormat"
  val Sparse           = "smithy.api#sparse"
  val Error            = "smithy.api#error"
  val HttpError        = "smithy.api#httpError"
  val Retryable        = "smithy.api#retryable"
  val IdempotencyToken = "smithy.api#idempotencyToken"
}

/**
  * A shape described as runtime data: its id, type, traits and members. Generated code builds one
  * schema per shape; codecs and protocols read them instead of being generated per shape.
  *
  * A member is a schema too. It has a name and an index, and it takes its type, members and
  * traits from its target, with the member's own traits layered on top.
  */
sealed abstract class Schema {
  def id: ShapeId
  def shapeType: ShapeType
  def traits: Traits
  def members: IndexedSeq[Schema]
  def memberName: String
  def memberIndex: Int

  final def isMember: Boolean = memberIndex >= 0
  final def hasTrait(traitId: String): Boolean = traits.has(traitId)
  final def traitValue(traitId: String): Option[String] = traits.get(traitId)

  private lazy val membersByName: Map[String, Schema] = members.map(m => m.memberName -> m).toMap
  final def member(name: String): Option[Schema] = membersByName.get(name)

  override def toString: String = s"Schema($id)"
}

final class ShapeSchema private[runtime] (
    val id: ShapeId,
    val shapeType: ShapeType,
    val traits: Traits,
    membersThunk: () => IndexedSeq[Schema]
) extends Schema {
  lazy val members: IndexedSeq[Schema] = membersThunk()
  def memberName: String               = ""
  def memberIndex: Int                 = -1
}

final class MemberSchema private[runtime] (
    container: ShapeId,
    val memberName: String,
    val memberIndex: Int,
    targetThunk: () => Schema,
    ownTraits: Traits
) extends Schema {
  lazy val target: Schema              = targetThunk()
  val id: ShapeId                      = ShapeId(container.namespace, s"${container.name}$$$memberName")
  def shapeType: ShapeType             = target.shapeType
  lazy val traits: Traits              = target.traits ++ ownTraits
  def members: IndexedSeq[Schema]      = target.members
}

/** A member as generated code declares it. The target is a thunk so recursive shapes work. */
final case class MemberDef(name: String, target: () => Schema, traits: Traits = Traits.empty)

object Schema {

  /** A simple shape: no members. */
  def simple(id: ShapeId, shapeType: ShapeType, traits: Traits = Traits.empty): Schema =
    new ShapeSchema(id, shapeType, traits, () => IndexedSeq.empty)

  /** A structure, union, list (one member, `member`) or map (`key` and `value`). */
  def aggregate(id: ShapeId, shapeType: ShapeType, traits: Traits = Traits.empty)(defs: MemberDef*): Schema = {
    val captured = defs.toVector
    new ShapeSchema(
      id,
      shapeType,
      traits,
      () => captured.zipWithIndex.map { case (d, i) => new MemberSchema(id, d.name, i, d.target, d.traits) }
    )
  }
}

/** Schemas for the prelude shapes (`smithy.api#String` and so on). */
object Prelude {
  private def p(name: String, t: ShapeType): Schema = Schema.simple(ShapeId("smithy.api", name), t)

  val Boolean: Schema    = p("Boolean", ShapeType.Boolean)
  val Byte: Schema       = p("Byte", ShapeType.Byte)
  val Short: Schema      = p("Short", ShapeType.Short)
  val Integer: Schema    = p("Integer", ShapeType.Integer)
  val Long: Schema       = p("Long", ShapeType.Long)
  val Float: Schema      = p("Float", ShapeType.Float)
  val Double: Schema     = p("Double", ShapeType.Double)
  val BigInteger: Schema = p("BigInteger", ShapeType.BigInteger)
  val BigDecimal: Schema = p("BigDecimal", ShapeType.BigDecimal)
  val String: Schema     = p("String", ShapeType.String)
  val Blob: Schema       = p("Blob", ShapeType.Blob)
  val Timestamp: Schema  = p("Timestamp", ShapeType.Timestamp)
  val Document: Schema   = p("Document", ShapeType.Document)
  val Unit: Schema       = Schema.aggregate(ShapeId("smithy.api", "Unit"), ShapeType.Structure)()
}

/** A Smithy `blob`: bytes with value equality. */
final class Blob private (private val bytes: Array[Byte]) derives CanEqual {
  def toArray: Array[Byte]    = bytes.clone()
  def size: Int               = bytes.length
  def asUtf8String: String    = new String(bytes, StandardCharsets.UTF_8)
  override def equals(other: Any): Boolean = other match {
    case b: Blob => java.util.Arrays.equals(bytes, b.bytes)
    case _       => false
  }
  override def hashCode: Int    = java.util.Arrays.hashCode(bytes)
  override def toString: String = s"Blob(${bytes.length} bytes)"
}

object Blob {
  val empty: Blob                       = new Blob(Array.emptyByteArray)
  def apply(bytes: Array[Byte]): Blob   = new Blob(bytes.clone())
  def fromUtf8(text: String): Blob      = new Blob(text.getBytes(StandardCharsets.UTF_8))
}

/** A Smithy `document`: schemaless JSON-like data. */
enum Document derives CanEqual {
  case DNull
  case DBoolean(value: scala.Boolean)
  case DNumber(value: scala.math.BigDecimal)
  case DString(value: java.lang.String)
  case DArray(values: scala.collection.immutable.List[Document])
  case DObject(fields: scala.collection.immutable.Map[java.lang.String, Document])
}
