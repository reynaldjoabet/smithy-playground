package smithyscala.hearth
package internal

import smithyscala.runtime.*

import scala.quoted.*

import hearth.MacroCommonsScala3

/** The Scala 3 adapter: Hearth's `MacroCommonsScala3` with the derivation mixed in. */
final private[hearth] class HearthCodecMacros(q: Quotes) extends MacroCommonsScala3(using q), HearthCodecMacrosImpl

private[hearth] object HearthCodecMacros {
  def deriveImpl[A: Type](schema: Expr[Schema])(using q: Quotes): Expr[SchemaCodec[A]] =
    new HearthCodecMacros(q).deriveCodec[A](schema)

  def enumValuesImpl[E: Type](using q: Quotes): Expr[List[E]] =
    new HearthCodecMacros(q).deriveEnumValues[E]
}
