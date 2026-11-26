package smithyscala.hearth

import java.util.UUID

import smithyscala.runtime.*

/**
  * Codecs derived with [Hearth](https://github.com/kubuszok/hearth): the alternative to the runtime's `Mirror`-based
  * `ShapeCodec.struct` / `ShapeCodec.union`. Generated code calls it when the generator's `derivation` setting is
  * `hearth`. See `internal.HearthCodecMacrosImpl` for how it works.
  */
object HearthCodec {

  /** A structure's or a union's codec, derived at compile time with direct writer/reader calls. */
  inline def derived[A](schema: Schema): SchemaCodec[A] = ${ internal.HearthCodecMacros.deriveImpl[A]('schema) }

  /** The singleton cases of an enum, in declaration order: everything but `UnknownValue(raw)`. */
  inline def enumValues[E]: List[E] = ${ internal.HearthCodecMacros.enumValuesImpl[E] }

  /** If a given of this type is in scope, `derived` reports how it derived each codec. */
  sealed trait LogDerivation
  object LogDerivation extends LogDerivation

  // ------------------------------------------------------------------------------------------ used by derived code

  /** Derived codecs are built through this factory, from lambdas, rather than one anonymous class per type. */
  def instance[A](
      schema: Schema,
      write: (ShapeWriter, Schema, A) => Unit,
      read: (ShapeReader, Schema) => A,
      absent: => Option[A]
  ): SchemaCodec[A] = new DerivedCodec[A](schema, write, read, () => absent)

  /** A required member that is missing and has no zero value (a structure). */
  def missing(member: Schema): Nothing =
    throw new SerializationException(s"Missing required member ${member.id}")

  def unknownUnionMember(union: Schema): Nothing =
    throw new SerializationException(s"Cannot serialize an unknown member of ${union.id}")

  /** The value an empty `@idempotencyToken` member gets. Input is serialized once per call, so retries reuse it. */
  def idempotencyToken(): String = UUID.randomUUID().toString
}

private final class DerivedCodec[A](
    val schema: Schema,
    writeFn: (ShapeWriter, Schema, A) => Unit,
    readFn: (ShapeReader, Schema) => A,
    absentFn: () => Option[A]
) extends SchemaCodec[A] {
  def write(w: ShapeWriter, s: Schema, value: A): Unit = writeFn(w, s, value)
  def read(r: ShapeReader, s: Schema): A               = readFn(r, s)
  override lazy val absent: Option[A]                  = absentFn()
}
