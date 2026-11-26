package smithyscala.codegen

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.knowledge.PaginatedIndex
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.loader.Prelude
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*
import software.amazon.smithy.protocol.traits.Rpcv2JsonTrait

import Naming.*

/**
  * Generates a Scala 3 client for one service: a case class, enum or type alias per shape (each
  * with a schema and schema-driven write/read methods), error classes, and the client.
  *
  * Generated code only describes shapes. The wire format lives in the runtime's codecs and
  * protocols, so supporting another protocol is a runtime change, not a generator change.
  */
object ScalaClientGenerator {

  /** Returns generated files as relative path -> source. */
  def generate(model: Model, serviceId: ShapeId, packageName: String): Map[String, String] =
    new Generator(model, model.expectShape(serviceId, classOf[ServiceShape]), packageName).files()
}

private final class Generator(model: Model, service: ServiceShape, pkg: String) {

  private val opIndex      = OperationIndex.of(model)
  private val serviceIndex = ServiceIndex.of(model)
  private val pagIndex     = PaginatedIndex.of(model)
  private val UnitId       = ShapeId.from("smithy.api#Unit")

  private val List_ = "scala.collection.immutable.List"
  private val Map_  = "scala.collection.immutable.Map"

  // ----------------------------------------------------------------------------------------------- shapes

  private val operations: List[OperationShape] =
    TopDownIndex.of(model).getContainedOperations(service).asScala.toList.sortBy(_.getId.getName)
      .filterNot(isEventStream)

  private val shapes: List[Shape] = new Walker(model)
    .walkShapes(service)
    .asScala
    .toList
    .filter(s => !Prelude.isPreludeShape(s.getId))
    .filter {
      case _: StructureShape | _: UnionShape | _: EnumShape | _: IntEnumShape | _: ListShape | _: MapShape => true
      case _                                                                                              => false
    }
    .sortBy(_.getId.toString)

  private def isEventStream(op: OperationShape): Boolean =
    Seq(op.getInputShape, op.getOutputShape).exists { id =>
      model.expectShape(id).members().asScala.exists { m =>
        model.expectShape(m.getTarget).hasTrait(classOf[StreamingTrait]) && model.expectShape(m.getTarget).isUnionShape
      }
    }

  private def typeName(id: ShapeId): String = service.getContextualName(model.expectShape(id))

  private def shapeIdExpr(id: ShapeId): String = s"rt.ShapeId(${lit(id.getNamespace)}, ${lit(id.getName)})"

  // ----------------------------------------------------------------------------------------------- types

  private enum Kind derives CanEqual {
    case Simple(method: String, scalaType: String, prelude: String)
    case EnumK(name: String)
    case IntEnumK(name: String)
    case Aggregate(name: String)
    case UnitK
  }

  private def kind(id: ShapeId): Kind =
    if (id.equals(UnitId)) Kind.UnitK
    else
      model.expectShape(id) match {
        case _: BooleanShape    => Kind.Simple("Boolean", "scala.Boolean", "Boolean")
        case _: ByteShape       => Kind.Simple("Byte", "scala.Byte", "Byte")
        case _: ShortShape      => Kind.Simple("Short", "scala.Short", "Short")
        case _: IntEnumShape    => Kind.IntEnumK(typeName(id))
        case _: IntegerShape    => Kind.Simple("Integer", "scala.Int", "Integer")
        case _: LongShape       => Kind.Simple("Long", "scala.Long", "Long")
        case _: FloatShape      => Kind.Simple("Float", "scala.Float", "Float")
        case _: DoubleShape     => Kind.Simple("Double", "scala.Double", "Double")
        case _: BigIntegerShape => Kind.Simple("BigInteger", "scala.math.BigInt", "BigInteger")
        case _: BigDecimalShape => Kind.Simple("BigDecimal", "scala.math.BigDecimal", "BigDecimal")
        case _: EnumShape       => Kind.EnumK(typeName(id))
        case _: StringShape     => Kind.Simple("String", "java.lang.String", "String")
        case _: BlobShape       => Kind.Simple("Blob", "rt.Blob", "Blob")
        case _: TimestampShape  => Kind.Simple("Timestamp", "java.time.Instant", "Timestamp")
        case _: DocumentShape   => Kind.Simple("Document", "rt.Document", "Document")
        case s                  => Kind.Aggregate(typeName(s.getId))
      }

  private def scalaType(id: ShapeId): String = kind(id) match {
    case Kind.Simple(_, t, _) => t
    case Kind.EnumK(n)        => n
    case Kind.IntEnumK(n)     => n
    case Kind.Aggregate(n)    => n
    case Kind.UnitK           => "scala.Unit"
  }

  private def writeStmt(w: String, m: String, x: String, id: ShapeId): String = kind(id) match {
    case Kind.Simple(method, _, _) => s"$w.write$method($m, $x)"
    case Kind.EnumK(_)             => s"$w.writeString($m, $x.value)"
    case Kind.IntEnumK(_)          => s"$w.writeInteger($m, $x.value)"
    case Kind.Aggregate(n)         => s"$n.write($w, $m, $x)"
    case Kind.UnitK                => s"$w.writeStruct($m)(_ => ())"
  }

  private def readExpr(r: String, m: String, id: ShapeId): String = kind(id) match {
    case Kind.Simple(method, _, _) => s"$r.read$method($m)"
    case Kind.EnumK(n)             => s"$n.fromValue($r.readString($m))"
    case Kind.IntEnumK(n)          => s"$n.fromValue($r.readInteger($m))"
    case Kind.Aggregate(n)         => s"$n.read($r, $m)"
    case Kind.UnitK                => "()"
  }

  /** The value used when a required member is missing from a response (Smithy's error correction). */
  private def zero(id: ShapeId, what: String): String = kind(id) match {
    case Kind.Simple(method, _, _) =>
      method match {
        case "Boolean"    => "false"
        case "Byte"       => "0.toByte"
        case "Short"      => "0.toShort"
        case "Integer"    => "0"
        case "Long"       => "0L"
        case "Float"      => "0f"
        case "Double"     => "0.0"
        case "BigInteger" => "scala.math.BigInt(0)"
        case "BigDecimal" => "scala.math.BigDecimal(0)"
        case "String"     => "\"\""
        case "Blob"       => "rt.Blob.empty"
        case "Timestamp"  => "java.time.Instant.EPOCH"
        case _            => "rt.Document.DNull"
      }
    case Kind.EnumK(n)  => s"$n.UnknownValue(\"\")"
    case Kind.IntEnumK(n) => s"$n.UnknownValue(0)"
    case Kind.UnitK     => "()"
    case Kind.Aggregate(n) =>
      model.expectShape(id) match {
        case _: ListShape  => "scala.Nil"
        case _: MapShape   => s"$Map_.empty"
        case _: UnionShape => s"$n.UnknownMember(\"\")"
        case _             => s"throw new rt.SerializationException(${lit(s"Missing required member $what")})"
      }
  }

  /** A Scala literal for a `@default` value, if this generator supports it. */
  private def defaultExpr(node: Node, id: ShapeId): Option[String] =
    if (node.isNullNode) None
    else
      (kind(id), model.expectShape(id)) match {
        case (Kind.Simple("Boolean", _, _), _) if node.isBooleanNode => Some(node.expectBooleanNode.getValue.toString)
        case (Kind.Simple(m, _, _), _) if node.isNumberNode =>
          val n = node.expectNumberNode.getValue
          m match {
            case "Byte"       => Some(s"${n.intValue}.toByte")
            case "Short"      => Some(s"${n.intValue}.toShort")
            case "Integer"    => Some(n.intValue.toString)
            case "Long"       => Some(s"${n.longValue}L")
            case "Float"      => Some(s"${n.floatValue}f")
            case "Double"     => Some(n.doubleValue.toString)
            case "BigInteger" => Some(s"scala.math.BigInt(${lit(n.toString)})")
            case "BigDecimal" => Some(s"scala.math.BigDecimal(${lit(n.toString)})")
            case _            => None
          }
        case (Kind.Simple("String", _, _), _) if node.isStringNode => Some(lit(node.expectStringNode.getValue))
        case (Kind.Simple("Blob", _, _), _) if node.isStringNode && node.expectStringNode.getValue.isEmpty =>
          Some("rt.Blob.empty")
        case (Kind.EnumK(n), _) if node.isStringNode => Some(s"$n.fromValue(${lit(node.expectStringNode.getValue)})")
        case (Kind.IntEnumK(n), _) if node.isNumberNode => Some(s"$n.fromValue(${node.expectNumberNode.getValue.intValue})")
        case (_, _: ListShape) if node.isArrayNode && node.expectArrayNode.isEmpty => Some("scala.Nil")
        case (_, _: MapShape) if node.isObjectNode && node.expectObjectNode.isEmpty => Some(s"$Map_.empty")
        case _ => None
      }

  // ----------------------------------------------------------------------------------------------- traits

  /** The traits the runtime reads: everything else stays in the model. */
  private def runtimeTraits(s: Shape): List[(String, String)] =
    List(
      s.getTrait(classOf[JsonNameTrait]).toScala.map(t => "smithy.api#jsonName" -> t.getValue),
      s.getTrait(classOf[TimestampFormatTrait]).toScala.map(t => "smithy.api#timestampFormat" -> t.getValue),
      s.getTrait(classOf[RequiredTrait]).toScala.map(_ => "smithy.api#required" -> ""),
      s.getTrait(classOf[SparseTrait]).toScala.map(_ => "smithy.api#sparse" -> ""),
      s.getTrait(classOf[ErrorTrait]).toScala.map(t => "smithy.api#error" -> t.getValue),
      s.getTrait(classOf[HttpErrorTrait]).toScala.map(t => "smithy.api#httpError" -> t.getCode.toString),
      s.getTrait(classOf[RetryableTrait]).toScala.map(t => "smithy.api#retryable" -> (if (t.getThrottling) "throttling" else "")),
      s.getTrait(classOf[IdempotencyTokenTrait]).toScala.map(_ => "smithy.api#idempotencyToken" -> "")
    ).flatten

  private def traitsExpr(traits: List[(String, String)]): String =
    if (traits.isEmpty) "rt.Traits.empty"
    else traits.map((k, v) => s"${lit(k)} -> ${lit(v)}").mkString("rt.Traits(", ", ", ")")

  /** A member definition. Simple targets use the prelude schema with the target's traits folded in. */
  private def memberDef(m: MemberShape): String = {
    val target = model.expectShape(m.getTarget)
    val (schemaExpr, extra) = kind(m.getTarget) match {
      case Kind.Simple(_, _, prelude) => (s"rt.Prelude.$prelude", runtimeTraits(target))
      case Kind.UnitK                 => ("rt.Prelude.Unit", Nil)
      case _                          => (s"${typeName(m.getTarget)}.schema", Nil)
    }
    val traits = extra ++ runtimeTraits(m)
    s"rt.MemberDef(${lit(m.getMemberName)}, () => $schemaExpr, ${traitsExpr(traits)})"
  }

  private def schemaVal(s: Shape, shapeType: String): String = {
    val members = s.members().asScala.toList
    val traits  = traitsExpr(runtimeTraits(s))
    if (members.isEmpty)
      s"  lazy val schema: rt.Schema = rt.Schema.aggregate(${shapeIdExpr(s.getId)}, rt.ShapeType.$shapeType, $traits)()\n"
    else
      s"  lazy val schema: rt.Schema = rt.Schema.aggregate(${shapeIdExpr(s.getId)}, rt.ShapeType.$shapeType, $traits)(\n" +
        members.map(m => s"    ${memberDef(m)}").mkString(",\n") + "\n  )\n"
  }

  private def doc(s: Shape, indent: String): String =
    s.getTrait(classOf[DocumentationTrait]).toScala.map(t => scaladoc(t.getValue, indent)).getOrElse("")

  // ----------------------------------------------------------------------------------------------- structures

  private enum Mode derives CanEqual {
    case Required
    case Optional
    case Default(expr: String)
  }

  private final case class Field(member: MemberShape, index: Int, name: String, elemType: String, mode: Mode) {
    def fieldType: String = mode match {
      case Mode.Optional => s"scala.Option[$elemType]"
      case _             => elemType
    }
  }

  private def fields(s: Shape): List[Field] = {
    val used = scala.collection.mutable.Set.empty[String]
    s.members().asScala.toList.zipWithIndex.map { (m, i) =>
      var name = field(m.getMemberName)
      while (used.contains(name)) name = name.stripSuffix("`") + "_"
      used += name
      val clientOptional = m.hasTrait(classOf[ClientOptionalTrait])
      val mode =
        if (m.hasTrait(classOf[RequiredTrait]) && !clientOptional) Mode.Required
        else
          m.getTrait(classOf[DefaultTrait]).toScala.filter(_ => !clientOptional).flatMap(t => defaultExpr(t.toNode, m.getTarget)) match {
            case Some(expr) => Mode.Default(expr)
            case None       => Mode.Optional
          }
      Field(m, i, name, scalaType(m.getTarget), mode)
    }
  }

  private def structure(s: StructureShape): String = {
    val name = typeName(s.getId)
    val fs   = fields(s)
    val params = fs.map { f =>
      val default = f.mode match {
        case Mode.Optional      => " = scala.None"
        case Mode.Default(expr) => s" = $expr"
        case Mode.Required      => ""
      }
      // `copy_: T` would lex as one identifier, `copy_:`.
      val colon = if (f.name.endsWith("_")) " :" else ":"
      s"    ${f.name}$colon ${f.fieldType}$default"
    }
    val error = s.getTrait(classOf[ErrorTrait]).toScala
    val extendsClause = error.fold("") { e =>
      val message = fs.find(f => f.member.getMemberName.equalsIgnoreCase("message") && f.elemType == "java.lang.String")
        .map(f => if (f.mode == Mode.Optional) s"${f.name}.orNull" else f.name)
        .getOrElse("null")
      s" extends rt.ServiceException(${shapeIdExpr(s.getId)}, $message, ${lit(e.getValue)})"
    }
    val retry = s.getTrait(classOf[RetryableTrait]).toScala.fold("") { t =>
      " {\n  override def smithyRetryable: scala.Boolean = true\n" +
        (if (t.getThrottling) "  override def smithyThrottling: scala.Boolean = true\n" else "") + "}"
    }
    val header =
      if (params.isEmpty) s"final case class $name()$extendsClause$retry\n"
      else s"final case class $name(\n${params.mkString(",\n")}\n)$extendsClause$retry\n"

    val writes = fs.map { f =>
      val m = s"ms(${f.index})"
      val v = s"value.${f.name}"
      f.mode match {
        case Mode.Optional if f.member.hasTrait(classOf[IdempotencyTokenTrait]) =>
          "      " + writeStmt("s", m, s"$v.getOrElse(java.util.UUID.randomUUID().toString)", f.member.getTarget)
        case Mode.Optional => s"      $v.foreach(x => ${writeStmt("s", m, "x", f.member.getTarget)})"
        case _             => "      " + writeStmt("s", m, v, f.member.getTarget)
      }
    }
    val write =
      if (fs.isEmpty) s"  def write(w: rt.ShapeWriter, schema: rt.Schema, value: $name): scala.Unit =\n    w.writeStruct(schema)(_ => ())\n"
      else
        s"  def write(w: rt.ShapeWriter, schema: rt.Schema, value: $name): scala.Unit =\n" +
          s"    w.writeStruct(schema) { s =>\n      val ms = $name.schema.members\n${writes.mkString("\n")}\n    }\n"

    val read =
      if (fs.isEmpty)
        s"  def read(r: rt.ShapeReader, schema: rt.Schema): $name = {\n    r.readStruct($name.schema)((_, _) => ())\n    $name()\n  }\n"
      else {
        val vars  = fs.map(f => s"    var f${f.index}: scala.Option[${f.elemType}] = scala.None")
        val cases = fs.map(f => s"          case ${f.index} => f${f.index} = scala.Some(${readExpr("mr", "m", f.member.getTarget)})")
        val args = fs.map { f =>
          f.mode match {
            case Mode.Optional      => s"f${f.index}"
            case Mode.Default(expr) => s"f${f.index}.getOrElse($expr)"
            case Mode.Required      => s"f${f.index}.getOrElse(${zero(f.member.getTarget, s"${s.getId.getName}.${f.member.getMemberName}")})"
          }
        }
        s"  def read(r: rt.ShapeReader, schema: rt.Schema): $name = {\n${vars.mkString("\n")}\n" +
          s"    r.readStruct($name.schema) { (m, mr) =>\n      if (!mr.isNull) {\n        m.memberIndex match {\n" +
          s"${cases.mkString("\n")}\n          case _ => ()\n        }\n      }\n    }\n" +
          s"    $name(\n${args.map("      " + _).mkString(",\n")}\n    )\n  }\n"
      }

    doc(s, "") + header + s"\nobject $name {\n" + schemaVal(s, "Structure") + "\n" + write + "\n" + read + "}\n"
  }

  // ----------------------------------------------------------------------------------------------- unions

  private def union(u: UnionShape): String = {
    val name    = typeName(u.getId)
    val members = u.members().asScala.toList.zipWithIndex
    val used    = scala.collection.mutable.Set("UnknownMember")
    val caseNames = members.map { (m, _) =>
      var n = escape(upperCamel(m.getMemberName))
      while (used.contains(n)) n = n.stripSuffix("`") + "_"
      used += n
      n
    }
    val cases = members.zip(caseNames).map { case ((m, _), c) =>
      if (m.getTarget.equals(UnitId)) s"  case $c" else s"  case $c(value: ${scalaType(m.getTarget)})"
    }
    val writeCases = members.zip(caseNames).map { case ((m, i), c) =>
      if (m.getTarget.equals(UnitId)) s"        case $name.$c => s.writeStruct(ms($i))(_ => ())"
      else s"        case $name.$c(x) => ${writeStmt("s", s"ms($i)", "x", m.getTarget)}"
    }
    val readCases = members.zip(caseNames).map { case ((m, i), c) =>
      if (m.getTarget.equals(UnitId)) s"          case $i => result = scala.Some($name.$c)"
      else s"          case $i => result = scala.Some($name.$c(${readExpr("mr", "m", m.getTarget)}))"
    }
    doc(u, "") +
      s"enum $name derives CanEqual {\n${cases.mkString("\n")}\n\n  /** A member this client doesn't know (added to the model later). */\n" +
      s"  case UnknownMember(name: java.lang.String)\n}\n\nobject $name {\n" + schemaVal(u, "Union") + "\n" +
      s"  def write(w: rt.ShapeWriter, schema: rt.Schema, value: $name): scala.Unit =\n" +
      s"    w.writeStruct(schema) { s =>\n      val ms = $name.schema.members\n      value match {\n${writeCases.mkString("\n")}\n" +
      s"        case $name.UnknownMember(n) => throw new rt.SerializationException(\"Cannot serialize unknown union member \" + n)\n" +
      s"      }\n    }\n\n" +
      s"  def read(r: rt.ShapeReader, schema: rt.Schema): $name = {\n" +
      s"    var result: scala.Option[$name] = scala.None\n    var unknown = \"\"\n" +
      s"    r.readStruct($name.schema, n => unknown = n) { (m, mr) =>\n      if (!mr.isNull) {\n        m.memberIndex match {\n" +
      s"${readCases.mkString("\n")}\n          case _ => ()\n        }\n      }\n    }\n" +
      s"    result.getOrElse($name.UnknownMember(unknown))\n  }\n}\n"
  }

  // ----------------------------------------------------------------------------------------------- enums

  private def enumeration(s: Shape, values: List[(String, String)], valueType: String, shapeType: String): String = {
    val name = typeName(s.getId)
    val used = scala.collection.mutable.Set("UnknownValue", "knownValues", "fromValue", "schema", "value", "ordinal")
    val cases = values.map { (memberName, literal) =>
      var n = escape(memberName)
      while (used.contains(n)) n = n.stripSuffix("`") + "_"
      used += n
      n -> literal
    }
    doc(s, "") +
      s"enum $name(val value: $valueType) derives CanEqual {\n" +
      cases.map((n, v) => s"  case $n extends $name($v)").mkString("\n") +
      s"\n\n  /** A value this client doesn't know (added to the model later). */\n  case UnknownValue(raw: $valueType) extends $name(raw)\n}\n\n" +
      s"object $name {\n  lazy val schema: rt.Schema = rt.Schema.simple(${shapeIdExpr(s.getId)}, rt.ShapeType.$shapeType)\n\n" +
      s"  lazy val knownValues: $List_[$name] = $List_(${cases.map(_._1).mkString(", ")})\n\n" +
      s"  def fromValue(value: $valueType): $name = knownValues.find(_.value == value).getOrElse(UnknownValue(value))\n}\n"
  }

  // ----------------------------------------------------------------------------------------------- collections

  private def list(l: ListShape): String = {
    val name   = typeName(l.getId)
    val target = l.getMember.getTarget
    val sparse = l.hasTrait(classOf[SparseTrait])
    val elem   = if (sparse) s"scala.Option[${scalaType(target)}]" else scalaType(target)
    val writeEach =
      if (sparse) s"value.foreach(e => e.fold(lw.writeNull(m))(x => ${writeStmt("lw", "m", "x", target)}))"
      else s"value.foreach(x => ${writeStmt("lw", "m", "x", target)})"
    val readEach =
      if (sparse) s"(b.addOne(if (er.isNull) scala.None else scala.Some(${readExpr("er", "m", target)})): scala.Unit)"
      else s"if (!er.isNull) (b.addOne(${readExpr("er", "m", target)}): scala.Unit)"
    doc(l, "") + s"type $name = $List_[$elem]\n\nobject $name {\n" + schemaVal(l, "List") + "\n" +
      s"  def write(w: rt.ShapeWriter, schema: rt.Schema, value: $name): scala.Unit =\n" +
      s"    w.writeList(schema, value.size) { lw =>\n      val m = $name.schema.members(0)\n      $writeEach\n    }\n\n" +
      s"  def read(r: rt.ShapeReader, schema: rt.Schema): $name = {\n      val m = $name.schema.members(0)\n" +
      s"    val b = $List_.newBuilder[$elem]\n    r.readList(schema)(er => $readEach)\n    b.result()\n  }\n}\n"
  }

  private def map(mp: MapShape): String = {
    val name   = typeName(mp.getId)
    val target = mp.getValue.getTarget
    val sparse = mp.hasTrait(classOf[SparseTrait])
    val v      = if (sparse) s"scala.Option[${scalaType(target)}]" else scalaType(target)
    val writeEach =
      if (sparse) s"value.foreach((k, e) => mw.writeEntry(k)(vw => e.fold(vw.writeNull(vm))(x => ${writeStmt("vw", "vm", "x", target)})))"
      else s"value.foreach((k, x) => mw.writeEntry(k)(vw => ${writeStmt("vw", "vm", "x", target)}))"
    val readEach =
      if (sparse) s"(b.addOne(k -> (if (vr.isNull) scala.None else scala.Some(${readExpr("vr", "vm", target)}))): scala.Unit)"
      else s"if (!vr.isNull) (b.addOne(k -> ${readExpr("vr", "vm", target)}): scala.Unit)"
    doc(mp, "") + s"type $name = $Map_[java.lang.String, $v]\n\nobject $name {\n" + schemaVal(mp, "Map") + "\n" +
      s"  def write(w: rt.ShapeWriter, schema: rt.Schema, value: $name): scala.Unit =\n" +
      s"    w.writeMap(schema, value.size) { mw =>\n      val vm = $name.schema.members(1)\n      $writeEach\n    }\n\n" +
      s"  def read(r: rt.ShapeReader, schema: rt.Schema): $name = {\n    val vm = $name.schema.members(1)\n" +
      s"    val b = $Map_.newBuilder[java.lang.String, $v]\n    r.readMap(schema)((k, vr) => $readEach)\n    b.result()\n  }\n}\n"
  }

  // ----------------------------------------------------------------------------------------------- client

  private def protocolExpr: String =
    if (service.hasTrait(classOf[AwsJson1_0Trait])) "rt.ProtocolInfo.AwsJson(\"1.0\")"
    else if (service.hasTrait(classOf[AwsJson1_1Trait])) "rt.ProtocolInfo.AwsJson(\"1.1\")"
    else if (service.hasTrait(classOf[Rpcv2JsonTrait])) "rt.ProtocolInfo.RpcV2Json"
    else {
      val found = ServiceIndex.of(model).getProtocols(service).keySet.asScala.mkString(", ")
      throw new UnsupportedOperationException(
        s"${service.getId}: no supported protocol (supported: awsJson1_0, awsJson1_1, rpcv2Json; the service has: $found)"
      )
    }

  private val sdkId: String =
    service.getTrait(classOf[ServiceTrait]).toScala.map(_.getSdkId).getOrElse(service.getId.getName)

  val clientName: String = sdkId.filter(_.isLetterOrDigit).capitalize + "Client"

  private def ioType(id: ShapeId): String = if (id.equals(UnitId)) "scala.Unit" else typeName(id)

  private def client(): String = {
    val ops = operations.map { op =>
      val in      = ioType(op.getInputShape)
      val out     = ioType(op.getOutputShape)
      val method  = escape(lowerCamel(op.getId.getName))
      val signature =
        if (op.getInputShape.equals(UnitId)) s"def $method(): $out" else s"def $method(input: $in): $out"
      (op, in, out, method, signature)
    }

    val paginators = ops.flatMap { (op, in, out, method, _) =>
      pagIndex.getPaginationInfo(service, op).toScala.flatMap { info =>
        val outPath = info.getOutputTokenMemberPath.asScala.toList
        val inField = fields(model.expectShape(op.getInputShape)).find(_.member.getMemberName == info.getInputTokenMember.getMemberName)
        val outField = outPath match {
          case single :: Nil => fields(model.expectShape(op.getOutputShape)).find(_.member.getMemberName == single.getMemberName)
          case _             => None
        }
        for {
          i <- inField if i.mode == Mode.Optional
          o <- outField
        } yield {
          val next = if (o.mode == Mode.Optional) s"out.${o.name}" else s"scala.Some(out.${o.name})"
          doc(op, "  ") +
            s"  def ${lowerCamel(op.getId.getName)}Paginator(input: $in): scala.collection.Iterator[$out] =\n" +
            s"    rt.Paginator.pages[$in, $out, ${i.elemType}](input, in => $method(in), out => $next, (in, t) => in.copy(${i.name} = scala.Some(t)))\n"
        }
      }
    }

    val operationVals = ops.map { (op, in, out, _, _) =>
      val writeIn =
        if (op.getInputShape.equals(UnitId)) "(w, _) => w.writeStruct(rt.Prelude.Unit)(_ => ())"
        else s"(w, in) => $in.write(w, $in.schema, in)"
      val readOut = if (op.getOutputShape.equals(UnitId)) "_ => ()" else s"r => $out.read(r, $out.schema)"
      val inSchema  = if (op.getInputShape.equals(UnitId)) "rt.Prelude.Unit" else s"$in.schema"
      val outSchema = if (op.getOutputShape.equals(UnitId)) "rt.Prelude.Unit" else s"$out.schema"
      val errors = opIndex.getErrors(service, op).asScala.toList.map { e =>
        val n = typeName(e.getId)
        s"        new rt.ErrorType($n.schema, r => $n.read(r, $n.schema))"
      }
      val auth = serviceIndex.getEffectiveAuthSchemes(service, op).keySet.asScala.toList.map(id => lit(id.toString))
      s"    lazy val ${op.getId.getName}: rt.Operation[$in, $out] = new rt.Operation[$in, $out](\n" +
        s"      ${shapeIdExpr(op.getId)},\n      $inSchema,\n      $writeIn,\n      $outSchema,\n      $readOut,\n" +
        s"      new rt.TypeRegistry($List_(\n${errors.mkString(",\n")}\n      )),\n" +
        s"      $List_(${auth.mkString(", ")})\n    )\n"
    }

    val impls = ops.map { (op, _, _, _, signature) =>
      val arg = if (op.getInputShape.equals(UnitId)) "()" else "input"
      s"    $signature = pipeline.invoke(Operations.${op.getId.getName}, $arg)"
    }

    val endpointPrefix =
      service.getTrait(classOf[ServiceTrait]).toScala.map(t => s"scala.Some(${lit(t.getEndpointPrefix)})").getOrElse("scala.None")
    val signingName =
      service.getTrait(classOf[SigV4Trait]).toScala.map(t => s"scala.Some(${lit(t.getName)})").getOrElse("scala.None")

    doc(service, "") +
      s"trait $clientName {\n" +
      ops.map((op, _, _, _, signature) => doc(op, "  ") + s"  $signature\n").mkString("\n") +
      (if (paginators.isEmpty) "" else "\n" + paginators.mkString("\n")) +
      s"}\n\nobject $clientName {\n" +
      s"  val service: rt.ServiceInfo = rt.ServiceInfo(\n    ${shapeIdExpr(service.getId)},\n    ${lit(sdkId)},\n" +
      s"    $endpointPrefix,\n    $signingName,\n    $protocolExpr\n  )\n\n" +
      s"  def apply(config: rt.ClientConfig = rt.ClientConfig()): $clientName = new Impl(new rt.ClientPipeline(service, config))\n\n" +
      s"  object Operations {\n${operationVals.mkString("\n")}  }\n\n" +
      s"  private final class Impl(pipeline: rt.ClientPipeline) extends $clientName {\n${impls.mkString("\n")}\n  }\n}\n"
  }

  // ----------------------------------------------------------------------------------------------- files

  private def file(body: String): String =
    s"// Generated by smithyscala.codegen from ${service.getId}. Do not edit.\npackage $pkg\n\nimport smithyscala.{runtime as rt}\n\n$body"

  def files(): Map[String, String] = {
    val dir = pkg.replace('.', '/')
    val sources = shapes.map { s =>
      val body = s match {
        case st: StructureShape => structure(st)
        case u: UnionShape      => union(u)
        case e: EnumShape =>
          enumeration(e, e.getEnumValues.asScala.toList.map((k, v) => k -> lit(v)), "java.lang.String", "Enum")
        case e: IntEnumShape =>
          enumeration(e, e.getEnumValues.asScala.toList.map((k, v) => k -> v.toString), "scala.Int", "IntEnum")
        case l: ListShape => list(l)
        case m: MapShape  => map(m)
        case other        => throw new IllegalStateException(s"unexpected shape ${other.getId}")
      }
      typeName(s.getId) -> file(body)
    } :+ (clientName -> file(client()))

    // macOS file systems ignore case: keep `Foo.scala` and `FOO.scala` apart.
    val seen = scala.collection.mutable.Map.empty[String, Int]
    sources.map { (n, src) =>
      val count = seen.getOrElse(n.toLowerCase, 0)
      seen(n.toLowerCase) = count + 1
      val fileName = if (count == 0) n else s"${n}_$count"
      s"$dir/$fileName.scala" -> src
    }.toMap
  }
}
