package smithyscala.runtime

import java.time.Instant
import java.util.UUID

import scala.compiletime.constValue
import scala.compiletime.erasedValue
import scala.compiletime.summonFrom
import scala.compiletime.summonInline
import scala.deriving.Mirror

/**
  * How one Scala type is written and read, given the schema of the member it sits in. The schema
  * carries the format-relevant traits; the codec only walks the value.
  *
  * Generated code doesn't implement this. Structures and unions derive it at compile time from
  * their `Mirror` (see [[ShapeCodec.struct]] and [[ShapeCodec.union]]); everything else is a
  * given below.
  */
trait ShapeCodec[A] {
  def write(w: ShapeWriter, schema: Schema, value: A): Unit
  def read(r: ShapeReader, schema: Schema): A

  /**
    * The value for a member the input doesn't contain: `None` for options, a zero value for
    * required members (Smithy's error correction), and nothing for structures, which then fail.
    */
  def absent: Option[A] = None

  /** True for values that a structure leaves out and a sparse collection writes as `null`. */
  def isEmpty(value: A): Boolean = false
}

/** A codec that also knows its own schema: structures, unions and `Unit`, which can stand alone. */
trait SchemaCodec[A] extends ShapeCodec[A] {
  def schema: Schema
}

object ShapeCodec {

  // ------------------------------------------------------------------------------------------ simple types

  private def simple[A](zero: A)(w: (ShapeWriter, Schema, A) => Unit, r: (ShapeReader, Schema) => A): ShapeCodec[A] =
    new ShapeCodec[A] {
      def write(writer: ShapeWriter, schema: Schema, value: A): Unit = w(writer, schema, value)
      def read(reader: ShapeReader, schema: Schema): A               = r(reader, schema)
      override val absent: Option[A]                                = Some(zero)
    }

  given ShapeCodec[Boolean]    = simple(false)(_.writeBoolean(_, _), _.readBoolean(_))
  given ShapeCodec[Byte]       = simple(0.toByte)(_.writeByte(_, _), _.readByte(_))
  given ShapeCodec[Short]      = simple(0.toShort)(_.writeShort(_, _), _.readShort(_))
  given ShapeCodec[Int]        = simple(0)(_.writeInteger(_, _), _.readInteger(_))
  given ShapeCodec[Long]       = simple(0L)(_.writeLong(_, _), _.readLong(_))
  given ShapeCodec[Float]      = simple(0f)(_.writeFloat(_, _), _.readFloat(_))
  given ShapeCodec[Double]     = simple(0.0)(_.writeDouble(_, _), _.readDouble(_))
  given ShapeCodec[BigInt]     = simple(BigInt(0))(_.writeBigInteger(_, _), _.readBigInteger(_))
  given ShapeCodec[BigDecimal] = simple(BigDecimal(0))(_.writeBigDecimal(_, _), _.readBigDecimal(_))
  given ShapeCodec[String]     = simple("")(_.writeString(_, _), _.readString(_))
  given ShapeCodec[Blob]       = simple(Blob.empty)(_.writeBlob(_, _), _.readBlob(_))
  given ShapeCodec[Instant]    = simple(Instant.EPOCH)(_.writeTimestamp(_, _), _.readTimestamp(_))
  given ShapeCodec[Document]   = simple(Document.DNull)(_.writeDocument(_, _), _.readDocument(_))

  /** `Unit` input and output, and `Unit` union members: an empty structure. */
  given unitCodec: SchemaCodec[Unit] with {
    val schema: Schema                                         = Prelude.Unit
    def write(w: ShapeWriter, s: Schema, value: Unit): Unit    = w.writeStruct(s)(_ => ())
    def read(r: ShapeReader, s: Schema): Unit                  = r.readStruct(Prelude.Unit)((_, _) => ())
  }

  // ------------------------------------------------------------------------------------------ containers

  // The element codec is a by-name context parameter, so recursive shapes (DynamoDB's
  // AttributeValue holds maps and lists of AttributeValue) resolve without an infinite loop.

  given optionCodec[A](using inner: => ShapeCodec[A]): ShapeCodec[Option[A]] with {
    private lazy val c                                                = inner
    def write(w: ShapeWriter, schema: Schema, value: Option[A]): Unit =
      value.fold(w.writeNull(schema))(c.write(w, schema, _))
    def read(r: ShapeReader, schema: Schema): Option[A] = if (r.isNull) None else Some(c.read(r, schema))
    override def absent: Option[Option[A]]               = Some(None)
    override def isEmpty(value: Option[A]): Boolean      = value.isEmpty
  }

  given listCodec[A](using inner: => ShapeCodec[A]): ShapeCodec[List[A]] with {
    private lazy val c = inner
    def write(w: ShapeWriter, schema: Schema, value: List[A]): Unit = {
      val m = schema.members(0)
      w.writeList(schema, value.size)(lw => value.foreach(x => if (c.isEmpty(x)) lw.writeNull(m) else c.write(lw, m, x)))
    }
    def read(r: ShapeReader, schema: Schema): List[A] = {
      val m = schema.members(0)
      val b = List.newBuilder[A]
      // A null element is kept only where the element type can hold it (a @sparse list of Options).
      r.readList(schema) { er =>
        if (!er.isNull) b.addOne(c.read(er, m)): Unit
        else c.absent.filter(c.isEmpty).foreach(b.addOne)
      }
      b.result()
    }
    override val absent: Option[List[A]] = Some(Nil)
  }

  given mapCodec[V](using inner: => ShapeCodec[V]): ShapeCodec[Map[String, V]] with {
    private lazy val c = inner
    def write(w: ShapeWriter, schema: Schema, value: Map[String, V]): Unit = {
      val m = schema.members(1)
      w.writeMap(schema, value.size) { mw =>
        value.foreach((k, x) => mw.writeEntry(k)(vw => if (c.isEmpty(x)) vw.writeNull(m) else c.write(vw, m, x)))
      }
    }
    def read(r: ShapeReader, schema: Schema): Map[String, V] = {
      val m = schema.members(1)
      val b = Map.newBuilder[String, V]
      r.readMap(schema) { (k, vr) =>
        if (!vr.isNull) b.addOne(k -> c.read(vr, m)): Unit
        else c.absent.filter(c.isEmpty).foreach(v => b.addOne(k -> v))
      }
      b.result()
    }
    override val absent: Option[Map[String, V]] = Some(Map.empty)
  }

  // ------------------------------------------------------------------------------------------ enums

  def stringEnum[E](fromValue: String => E, value: E => String): ShapeCodec[E] = new ShapeCodec[E] {
    def write(w: ShapeWriter, schema: Schema, e: E): Unit = w.writeString(schema, value(e))
    def read(r: ShapeReader, schema: Schema): E           = fromValue(r.readString(schema))
    override lazy val absent: Option[E]                   = Some(fromValue(""))
  }

  def intEnum[E](fromValue: Int => E, value: E => Int): ShapeCodec[E] = new ShapeCodec[E] {
    def write(w: ShapeWriter, schema: Schema, e: E): Unit = w.writeInteger(schema, value(e))
    def read(r: ShapeReader, schema: Schema): E           = fromValue(r.readInteger(schema))
    override lazy val absent: Option[E]                   = Some(fromValue(0))
  }

  // ------------------------------------------------------------------------------------------ structures

  /**
    * Derives a structure codec at compile time. The `Mirror` gives the field types in declaration
    * order, which the generator keeps equal to the schema's member order; each field's codec is
    * found by implicit search when this is inlined, and looked up lazily at runtime.
    */
  inline def struct[A <: Product](schema: Schema)(using m: Mirror.ProductOf[A]): SchemaCodec[A] =
    new StructCodec[A](schema, m, () => fieldCodecs[m.MirroredElemTypes], Defaults.of[A])

  private inline def fieldCodecs[T <: Tuple]: List[ShapeCodec[Any]] = inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t)   => summonInline[ShapeCodec[h]].asInstanceOf[ShapeCodec[Any]] :: fieldCodecs[t]
  }

  // ------------------------------------------------------------------------------------------ unions

  /**
    * Derives a union codec from the enum's `Mirror`: one case per member, in member order, then
    * `UnknownMember(name)`. A case is a singleton (a `Unit` member) or has one field, `value`.
    */
  inline def union[U](schema: Schema)(using m: Mirror.SumOf[U]): SchemaCodec[U] =
    new UnionCodec[U](schema, () => unionCases[m.MirroredElemTypes, U])

  private inline def unionCases[Cs <: Tuple, U]: List[UnionCase[U]] = inline erasedValue[Cs] match {
    case _: EmptyTuple => Nil
    case _: (c *: cs)  => unionCase[c, U](using summonInline[Mirror.ProductOf[c]]) :: unionCases[cs, U]
  }

  private inline def unionCase[C, U](using m: Mirror.ProductOf[C]): UnionCase[U] =
    inline erasedValue[m.MirroredElemTypes] match {
      case _: EmptyTuple =>
        UnionCase[U](constValue[m.MirroredLabel], None, _ => m.fromProduct(EmptyTuple).asInstanceOf[U])
      case _: (v *: EmptyTuple) =>
        UnionCase[U](
          constValue[m.MirroredLabel],
          Some(summonInline[ShapeCodec[v]].asInstanceOf[ShapeCodec[Any]]),
          x => m.fromProduct(Tuple1(x)).asInstanceOf[U]
        )
    }

  // ------------------------------------------------------------------------------------------ enum values

  /** The singleton cases of an enum, in order: everything but `UnknownValue(raw)`. */
  inline def enumValues[E](using m: Mirror.SumOf[E]): List[E] = singletons[m.MirroredElemTypes, E]

  private inline def singletons[Cs <: Tuple, E]: List[E] = inline erasedValue[Cs] match {
    case _: EmptyTuple => Nil
    case _: (c *: cs)  => singleton[c, E] ++ singletons[cs, E]
  }

  private inline def singleton[C, E]: List[E] = summonFrom {
    case v: ValueOf[C] => List(v.value.asInstanceOf[E])
    case _             => Nil
  }
}

private final class StructCodec[A](
    val schema: Schema,
    mirror: Mirror.ProductOf[A],
    codecsThunk: () => List[ShapeCodec[Any]],
    defaults: Map[Int, Any]
) extends SchemaCodec[A] {
  private lazy val codecs: IArray[ShapeCodec[Any]] = IArray.from(codecsThunk())
  private lazy val members: IndexedSeq[Schema]     = schema.members
  private lazy val tokens: IArray[Boolean]         = IArray.from(members.map(_.hasTrait(Traits.IdempotencyToken)))

  def write(w: ShapeWriter, s: Schema, value: A): Unit = {
    val product = value.asInstanceOf[Product]
    w.writeStruct(s) { sw =>
      for (i <- members.indices) {
        val v = product.productElement(i)
        if (!codecs(i).isEmpty(v)) codecs(i).write(sw, members(i), v)
        // An empty @idempotencyToken member gets a fresh token. Input is serialized once per
        // call, so every retry carries the same token.
        else if (tokens(i)) sw.writeString(members(i), UUID.randomUUID().toString)
      }
    }
  }

  def read(r: ShapeReader, s: Schema): A = {
    val values = new Array[Any](members.size)
    r.readStruct(schema) { (m, mr) =>
      if (!mr.isNull) values(m.memberIndex) = codecs(m.memberIndex).read(mr, m)
    }
    for (i <- values.indices if values(i).asInstanceOf[AnyRef] eq null) {
      values(i) = defaults.get(i).orElse(codecs(i).absent).getOrElse {
        throw new SerializationException(s"Missing required member ${members(i).id}")
      }
    }
    mirror.fromProduct(Tuple.fromArray(values.map(_.asInstanceOf[Object])))
  }
}

private final case class UnionCase[U](name: String, codec: Option[ShapeCodec[Any]], make: Any => U)

private final class UnionCodec[U](val schema: Schema, casesThunk: () => List[UnionCase[U]]) extends SchemaCodec[U] {
  private lazy val cases: IndexedSeq[UnionCase[U]] = casesThunk().toIndexedSeq
  private lazy val unknown: UnionCase[U] =
    cases.lastOption.filter(_.name == "UnknownMember").getOrElse(sys.error(s"${schema.id}: no UnknownMember case"))

  def write(w: ShapeWriter, s: Schema, value: U): Unit = {
    val ordinal = value.asInstanceOf[scala.reflect.Enum].ordinal
    val c       = cases(ordinal)
    if (c eq unknown) throw new SerializationException(s"Cannot serialize an unknown member of ${schema.id}")
    w.writeStruct(s) { sw =>
      c.codec match {
        case Some(codec) => codec.write(sw, schema.members(ordinal), value.asInstanceOf[Product].productElement(0))
        case None        => sw.writeStruct(schema.members(ordinal))(_ => ())
      }
    }
  }

  def read(r: ShapeReader, s: Schema): U = {
    var result: Option[U] = None
    var unknownName       = ""
    r.readStruct(schema, n => unknownName = n) { (m, mr) =>
      if (!mr.isNull) {
        val c = cases(m.memberIndex)
        result = Some(c.make(c.codec.fold(())(_.read(mr, m))))
      }
    }
    result.getOrElse(unknown.make(unknownName))
  }

  override lazy val absent: Option[U] = Some(unknown.make(""))
}
