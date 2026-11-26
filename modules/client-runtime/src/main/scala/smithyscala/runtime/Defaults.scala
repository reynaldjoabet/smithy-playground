package smithyscala.runtime

import scala.quoted.*

/**
  * Reads a case class's default parameter values at compile time.
  *
  * `Mirror` gives field types and labels but not defaults, so this is a quoted macro: it finds
  * the constructor parameters marked `HasDefault` and calls the companion's synthetic
  * `$lessinit$greater$default$N` getters. Generated structures use it so that a `@default`
  * member missing from a response gets the model's default value.
  */
object Defaults {

  /** Field index -> default value, for every field that has one. */
  inline def of[A]: Map[Int, Any] = ${ ofImpl[A] }

  def ofImpl[A: Type](using Quotes): Expr[Map[Int, Any]] = {
    import quotes.reflect.*
    val cls       = TypeRepr.of[A].typeSymbol
    val companion = cls.companionModule
    val params    = cls.primaryConstructor.paramSymss.flatten.filterNot(_.isTypeParam)
    val entries = params.zipWithIndex.collect {
      case (p, i) if p.flags.is(Flags.HasDefault) =>
        val getter = companion.declaredMethod(s"$$lessinit$$greater$$default$$${i + 1}").headOption.getOrElse {
          report.errorAndAbort(s"${cls.name}.${p.name} has a default but no default getter")
        }
        '{ (${ Expr(i) }, ${ Ref(companion).select(getter).asExprOf[Any] }) }
    }
    '{ Map[Int, Any](${ Varargs(entries) }*) }
  }
}
