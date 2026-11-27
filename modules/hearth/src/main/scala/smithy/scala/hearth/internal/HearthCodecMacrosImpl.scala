package smithy.scala.hearth
package internal

import smithy.scala.runtime.*

import hearth.MacroCommons
import hearth.fp.effect.*
import hearth.fp.instances.*
import hearth.fp.syntax.*
import hearth.std.*

/**
  * Compile-time derivation of [[SchemaCodec]] with Hearth, following its "sanely-automatic" pattern:
  *
  *   - the macro walks the type itself (simple types, options, lists, maps, structures, unions) and emits direct
  *     calls such as `w.writeString(member, value.name)`: no reflection, no boxing, no per-field codec objects;
  *   - a codec someone provides for a type (generated code, or the user) is used instead of deriving it, so any shape
  *     can be overridden; the library's own fallback instances are ignored, and so is the type being derived, which
  *     is reached through a cached helper `def` instead (recursive shapes such as DynamoDB's `AttributeValue`);
  *   - every type gets one helper `def` per direction, declared before its body is derived, so each is derived once
  *     and recursion needs no tricks;
  *   - rules say why they did not apply; failures are collected across fields and cases and rendered once, with a
  *     hint on how to see the derivation log (`import smithy.scala.hearth.debug.logDerivation`, or
  *     `-Xmacro-settings:smithy.scala.hearth.logDerivation=true`).
  *
  * It is the alternative to the runtime's own `Mirror`-based `ShapeCodec.struct` / `ShapeCodec.union`; the generator
  * picks one with its `derivation` setting.
  */
private[hearth] trait HearthCodecMacrosImpl { this: MacroCommons & StdExtensions =>

  // ------------------------------------------------------------------------------------------------ errors

  sealed trait DerivationError extends scala.util.control.NoStackTrace with Product with Serializable {
    def message: String
    override def getMessage: String = message
  }
  object DerivationError {
    final case class UnsupportedType(tpe: String, reasons: List[String]) extends DerivationError {
      def message: String = s"No rule could derive a codec for $tpe:\n${reasons.map("  - " + _).mkString("\n")}"
    }
    final case class UnsupportedMapKey(tpe: String, key: String) extends DerivationError {
      def message: String = s"$tpe: Smithy maps have String keys, but this one has $key"
    }
    final case class UnsupportedCollection(tpe: String, built: String) extends DerivationError {
      def message: String = s"$tpe: its builder produces $built, which is not a $tpe"
    }
    final case class UnsupportedUnionCase(union: String, caseName: String) extends DerivationError {
      def message: String =
        s"$union.$caseName: a union case must be a singleton (a Unit member) or have exactly one field"
    }
    final case class NotConstructible(tpe: String) extends DerivationError {
      def message: String = s"$tpe: its primary constructor is not accessible here"
    }
  }

  // ------------------------------------------------------------------------------------------------ types

  /** Kept out of implicit scope, so that a `Type[X]` never resolves to itself (Hearth's recommendation). */
  private object Types {
    val Writer: Type[ShapeWriter]            = Type.of[ShapeWriter]
    val Reader: Type[ShapeReader]            = Type.of[ShapeReader]
    val Schema: Type[Schema]                 = Type.of[Schema]
    val Members: Type[IndexedSeq[Schema]]    = Type.of[IndexedSeq[Schema]]
    val Unit: Type[Unit]                     = Type.of[Unit]
    val Boolean: Type[Boolean]               = Type.of[Boolean]
    val String: Type[String]                 = Type.of[String]
    val Codec: Type.Ctor1[ShapeCodec]        = Type.Ctor1.of[ShapeCodec]
    val SchemaCodec: Type.Ctor1[SchemaCodec] = Type.Ctor1.of[SchemaCodec]
    val Option: Type.Ctor1[Option]           = Type.Ctor1.of[Option]
    val List: Type.Ctor1[List]               = Type.Ctor1.of[List]
    val LogDerivation: Type[HearthCodec.LogDerivation] = Type.of[HearthCodec.LogDerivation]
  }

  /** The runtime's fallback instances and this module's own methods: never summoned by the derivation. */
  private lazy val ignoredImplicits: Seq[UntypedMethod] =
    Type.of[ShapeCodec.type].asUntyped.methods ++ Type.of[HearthCodec.type].asUntyped.methods

  // ------------------------------------------------------------------------------------------------ entry points

  def deriveCodec[A: Type](schema: Expr[Schema]): Expr[SchemaCodec[A]] = {
    implicit val SchemaT: Type[Schema]        = Types.Schema
    implicit val WriterT: Type[ShapeWriter]   = Types.Writer
    implicit val ReaderT: Type[ShapeReader]   = Types.Reader
    implicit val UnitT: Type[Unit]            = Types.Unit
    implicit val CodecA: Type[SchemaCodec[A]] = Types.SchemaCodec[A]
    implicit val OptionA: Type[Option[A]]     = Types.Option[A]
    run[A, SchemaCodec[A]]("HearthCodec.derived") {
      val shared = Shared(ValDefsCache.mlocal, Type[A].as_??)
      for {
        _      <- loadExtensionsOnce
        write  <- writer[A](shared)
        read   <- reader[A](shared)
        absent <- absentOf[A]
        cache  <- shared.cache.get
      } yield cache.toValDefs.use { _ =>
        Expr.quote {
          smithy.scala.hearth.HearthCodec.instance[A](
            Expr.splice(schema),
            (w: ShapeWriter, s: Schema, v: A) => Expr.splice(write(Expr.quote(w), Expr.quote(s), Expr.quote(v))),
            (r: ShapeReader, s: Schema) => Expr.splice(read(Expr.quote(r), Expr.quote(s))),
            Expr.splice(absent)
          )
        }
      }
    }
  }

  /** The singleton cases of an enum, in declaration order: every value but `UnknownValue(raw)`. */
  def deriveEnumValues[E: Type]: Expr[List[E]] = {
    implicit val ListE: Type[List[E]] = Types.List[E]
    run[E, List[E]]("HearthCodec.enumValues") {
      Enum.parse[E].toEither match {
        case Left(reason) => MIO.fail(DerivationError.UnsupportedType(Type.prettyPrint[E], List(reason)))
        case Right(enumm) =>
          val singletons: List[Expr[E]] = enumm.directChildren.toList.flatMap { case (_, child) =>
            import child.Underlying as C
            Expr.singletonOf[C].map(_.upcast[E])
          }
          MIO.pure(singletons.foldRight(Expr.quote(List.empty[E])) { (head, tail) =>
            Expr.quote(Expr.splice(head) :: Expr.splice(tail))
          })
      }
    }
  }

  private def run[A: Type, Out: Type](macroName: String)(derive: => MIO[Expr[Out]]): Expr[Out] = {
    val log = shouldLogDerivation
    Log
      .namedScope(s"$macroName[${Type.prettyPrint[A]}] at ${Environment.currentPosition.prettyPrint}")(derive)
      .runToExprOrFail(
        macroName,
        infoRendering = if (log) RenderFrom(Log.Level.Info) else DontRender,
        errorRendering = if (log) RenderFrom(Log.Level.Info) else DontRender,
        timeout = scala.concurrent.duration.FiniteDuration(30, java.util.concurrent.TimeUnit.SECONDS)
      ) { (logs, errors) =>
        val rendered = errors.toVector.map(_.getMessage).mkString("\n")
        val hint = "See how it was derived with: import smithy.scala.hearth.debug.logDerivation, " +
          "or the scalac option -Xmacro-settings:smithy.scala.hearth.logDerivation=true"
        if (logs.nonEmpty) s"$macroName failed for ${Type.plainPrint[A]}:\n$rendered\nDerivation log:\n$logs\n$hint"
        else s"$macroName failed for ${Type.plainPrint[A]}:\n$rendered\n$hint"
      }
  }

  private def shouldLogDerivation: Boolean = {
    implicit val LogT: Type[HearthCodec.LogDerivation] = Types.LogDerivation
    def imported = Expr.summonImplicit[HearthCodec.LogDerivation].isDefined
    def setting = (for {
      data   <- Environment.typedSettings.toOption
      smithy <- data.get("smithy")
      sc     <- smithy.get("scala")
      scope  <- sc.get("hearth")
      flag   <- scope.get("logDerivation").flatMap(_.asBoolean)
    } yield flag).getOrElse(false)
    imported || setting
  }

  private var extensionsLoaded = false
  private def loadExtensionsOnce: MIO[Unit] =
    if (extensionsLoaded) MIO.pure(())
    else Environment.loadStandardExtensions().toMIO(allowFailures = false).map(_ => extensionsLoaded = true)

  // ------------------------------------------------------------------------------------------------ shared state

  /** The cache of helper defs and lazy vals for one derived instance, and the type being derived. */
  final case class Shared(cache: MLocal[ValDefsCache], self: ??)

  private type WriteCall[B] = (Expr[ShapeWriter], Expr[Schema], Expr[B]) => Expr[Unit]
  private type ReadCall[B]  = (Expr[ShapeReader], Expr[Schema]) => Expr[B]

  /** A summoned codec, held in a lazy val of the instance: resolved once, not on every call. */
  private def cachedInstance[B: Type](shared: Shared, instance: Expr[ShapeCodec[B]]): MIO[Expr[ShapeCodec[B]]] = {
    implicit val CodecB: Type[ShapeCodec[B]] = Types.Codec[B]
    shared.cache.get0Ary[ShapeCodec[B]]("instance").flatMap {
      case Some(cached) => MIO.pure(cached)
      case None =>
        shared.cache.buildCachedWith("instance", ValDefBuilder.ofLazy[ShapeCodec[B]](s"codec_${Type.shortName[B]}")) {
          _ => instance
        } >> shared.cache.get0Ary[ShapeCodec[B]]("instance").map(_.get)
    }
  }

  /** A codec someone else provides for `B`: generated code, or the user. Never `B` itself, nor library fallbacks. */
  private def provided[B: Type](shared: Shared): MIO[Option[Expr[ShapeCodec[B]]]] = {
    implicit val CodecB: Type[ShapeCodec[B]] = Types.Codec[B]
    import shared.self.Underlying as Self
    if (Type[B] =:= Type[Self]) MIO.pure(None)
    else
      Expr.summonImplicitIgnoring[ShapeCodec[B]](ignoredImplicits*).toOption match {
        case Some(instance) =>
          Log.info(s"Using the provided ${Type.prettyPrint[ShapeCodec[B]]}") >>
            cachedInstance[B](shared, instance).map(Some(_))
        case None => MIO.pure(None)
      }
  }

  // ------------------------------------------------------------------------------------------------ simple types

  /** A Smithy simple type: its writer/reader call, and its zero value for a missing required member. */
  private final case class Simple[B](write: WriteCall[B], read: ReadCall[B], zero: Expr[B])

  private def simple[B: Type]: Option[Simple[B]] = {
    def as[X](s: Simple[X]): Option[Simple[B]] = Some(s.asInstanceOf[Simple[B]])
    if (Type[B] =:= Type.of[Boolean])
      as(Simple[Boolean]((w, s, v) => Expr.quote(Expr.splice(w).writeBoolean(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readBoolean(Expr.splice(s))), Expr.quote(false)))
    else if (Type[B] =:= Type.of[Byte])
      as(Simple[Byte]((w, s, v) => Expr.quote(Expr.splice(w).writeByte(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readByte(Expr.splice(s))), Expr.quote(0.toByte)))
    else if (Type[B] =:= Type.of[Short])
      as(Simple[Short]((w, s, v) => Expr.quote(Expr.splice(w).writeShort(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readShort(Expr.splice(s))), Expr.quote(0.toShort)))
    else if (Type[B] =:= Type.of[Int])
      as(Simple[Int]((w, s, v) => Expr.quote(Expr.splice(w).writeInteger(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readInteger(Expr.splice(s))), Expr.quote(0)))
    else if (Type[B] =:= Type.of[Long])
      as(Simple[Long]((w, s, v) => Expr.quote(Expr.splice(w).writeLong(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readLong(Expr.splice(s))), Expr.quote(0L)))
    else if (Type[B] =:= Type.of[Float])
      as(Simple[Float]((w, s, v) => Expr.quote(Expr.splice(w).writeFloat(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readFloat(Expr.splice(s))), Expr.quote(0f)))
    else if (Type[B] =:= Type.of[Double])
      as(Simple[Double]((w, s, v) => Expr.quote(Expr.splice(w).writeDouble(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readDouble(Expr.splice(s))), Expr.quote(0.0)))
    else if (Type[B] =:= Type.of[BigInt])
      as(Simple[BigInt]((w, s, v) => Expr.quote(Expr.splice(w).writeBigInteger(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readBigInteger(Expr.splice(s))), Expr.quote(BigInt(0))))
    else if (Type[B] =:= Type.of[BigDecimal])
      as(Simple[BigDecimal]((w, s, v) => Expr.quote(Expr.splice(w).writeBigDecimal(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readBigDecimal(Expr.splice(s))), Expr.quote(BigDecimal(0))))
    else if (Type[B] =:= Type.of[String])
      as(Simple[String]((w, s, v) => Expr.quote(Expr.splice(w).writeString(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readString(Expr.splice(s))), Expr.quote("")))
    else if (Type[B] =:= Type.of[Blob])
      as(Simple[Blob]((w, s, v) => Expr.quote(Expr.splice(w).writeBlob(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readBlob(Expr.splice(s))), Expr.quote(smithy.scala.runtime.Blob.empty)))
    else if (Type[B] =:= Type.of[java.time.Instant])
      as(Simple[java.time.Instant]((w, s, v) => Expr.quote(Expr.splice(w).writeTimestamp(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readTimestamp(Expr.splice(s))), Expr.quote(java.time.Instant.EPOCH)))
    else if (Type[B] =:= Type.of[Document])
      as(Simple[Document]((w, s, v) => Expr.quote(Expr.splice(w).writeDocument(Expr.splice(s), Expr.splice(v))),
        (r, s) => Expr.quote(Expr.splice(r).readDocument(Expr.splice(s))), Expr.quote(smithy.scala.runtime.Document.DNull)))
    else None
  }

  // ------------------------------------------------------------------------------------------------ helper defs

  /** `write_B(w, s, v)`: derived once per type, declared before its body so that recursion finds it. */
  private def writer[B: Type](shared: Shared): MIO[WriteCall[B]] = {
    implicit val WriterT: Type[ShapeWriter] = Types.Writer
    implicit val SchemaT: Type[Schema]      = Types.Schema
    implicit val UnitT: Type[Unit]          = Types.Unit
    shared.cache.get3Ary[ShapeWriter, Schema, B, Unit]("write").flatMap {
      case Some(call) => Log.info(s"Reusing write_${Type.shortName[B]}") >> MIO.pure(call)
      case None =>
        val builder = ValDefBuilder.ofDef3[ShapeWriter, Schema, B, Unit](s"write_${Type.shortName[B]}")
        for {
          _ <- shared.cache.forwardDeclare("write", builder)
          _ <- MIO.scoped { runSafe =>
            runSafe(shared.cache.buildCachedWith("write", builder) { case (_, (w, s, v)) =>
              runSafe(deriveWrite[B](shared, w, s, v))
            })
          }
          call <- shared.cache.get3Ary[ShapeWriter, Schema, B, Unit]("write")
        } yield call.get
    }
  }

  /** `read_B(r, s)`, like [[writer]]. */
  private def reader[B: Type](shared: Shared): MIO[ReadCall[B]] = {
    implicit val ReaderT: Type[ShapeReader] = Types.Reader
    implicit val SchemaT: Type[Schema]      = Types.Schema
    shared.cache.get2Ary[ShapeReader, Schema, B]("read").flatMap {
      case Some(call) => Log.info(s"Reusing read_${Type.shortName[B]}") >> MIO.pure(call)
      case None =>
        val builder = ValDefBuilder.ofDef2[ShapeReader, Schema, B](s"read_${Type.shortName[B]}")
        for {
          _ <- shared.cache.forwardDeclare("read", builder)
          _ <- MIO.scoped { runSafe =>
            runSafe(shared.cache.buildCachedWith("read", builder) { case (_, (r, s)) =>
              runSafe(deriveRead[B](shared, r, s))
            })
          }
          call <- shared.cache.get2Ary[ShapeReader, Schema, B]("read")
        } yield call.get
    }
  }

  // ------------------------------------------------------------------------------------------------ rules

  private type Attempt[T] = MIO[Either[String, T]]

  /** Rules in priority order: the first that applies wins; if none does, every rule's reason is reported. */
  private def firstRule[T](tpe: String, rules: List[(String, () => Attempt[T])]): MIO[T] = {
    def loop(rest: List[(String, () => Attempt[T])], reasons: List[String]): MIO[T] = rest match {
      case Nil => MIO.fail(DerivationError.UnsupportedType(tpe, reasons.reverse))
      case (name, rule) :: tail =>
        rule().flatMap {
          case Right(result) => Log.info(s"$tpe: handled as $name") >> MIO.pure(result)
          case Left(reason)  => Log.info(s"$tpe: not $name ($reason)") >> loop(tail, s"not $name: $reason" :: reasons)
        }
    }
    loop(rules, Nil)
  }

  private def deriveWrite[B: Type](shared: Shared, w: Expr[ShapeWriter], s: Expr[Schema], v: Expr[B]): MIO[Expr[Unit]] =
    Log.namedScope(s"write ${Type.prettyPrint[B]}") {
      implicit val UnitT: Type[Unit] = Types.Unit
      firstRule[Expr[Unit]](
        Type.plainPrint[B],
        List(
          "a provided ShapeCodec" -> (() =>
            provided[B](shared).map(_.toRight("none in implicit scope").map { codec =>
              Expr.quote(Expr.splice(codec).write(Expr.splice(w), Expr.splice(s), Expr.splice(v)))
            })
          ),
          "a Smithy simple type" -> (() => MIO.pure(simple[B].toRight("not one of them").map(_.write(w, s, v)))),
          "an Option" -> (() => writeOption[B](shared, w, s, v)),
          "a list or a map" -> (() => writeCollection[B](shared, w, s, v)),
          "a structure" -> (() => writeStructure[B](shared, w, s, v)),
          "a union" -> (() => writeUnion[B](shared, w, s, v))
        )
      )
    }

  private def deriveRead[B: Type](shared: Shared, r: Expr[ShapeReader], s: Expr[Schema]): MIO[Expr[B]] =
    Log.namedScope(s"read ${Type.prettyPrint[B]}") {
      firstRule[Expr[B]](
        Type.plainPrint[B],
        List(
          "a provided ShapeCodec" -> (() =>
            provided[B](shared).map(_.toRight("none in implicit scope").map { codec =>
              Expr.quote(Expr.splice(codec).read(Expr.splice(r), Expr.splice(s)))
            })
          ),
          "a Smithy simple type" -> (() => MIO.pure(simple[B].toRight("not one of them").map(_.read(r, s)))),
          "an Option" -> (() => readOption[B](shared, r, s)),
          "a list or a map" -> (() => readCollection[B](shared, r, s)),
          "a structure" -> (() => readStructure[B](shared, r, s)),
          "a union" -> (() => readUnion[B](shared, r, s))
        )
      )
    }

  // ------------------------------------------------------------------------------------------------ Option

  /** In a list or map an empty Option is a null (a `@sparse` collection); structures leave it out instead. */
  private def writeOption[B: Type](shared: Shared, w: Expr[ShapeWriter], s: Expr[Schema], v: Expr[B]): Attempt[Expr[Unit]] =
    Type[B] match {
      case IsOption(isOption) =>
        import isOption.Underlying as Item
        implicit val UnitT: Type[Unit] = Types.Unit
        writer[Item](shared).map { write =>
          Right(isOption.value.fold[Unit](v)(
            onEmpty = Expr.quote(Expr.splice(w).writeNull(Expr.splice(s))),
            onSome = item => write(w, s, item)
          ))
        }
      case _ => MIO.pure(Left("not an Option"))
    }

  private def readOption[B: Type](shared: Shared, r: Expr[ShapeReader], s: Expr[Schema]): Attempt[Expr[B]] =
    Type[B] match {
      case IsOption(isOption) =>
        import isOption.Underlying as Item
        reader[Item](shared).map { read =>
          Right(Expr.quote {
            if (Expr.splice(r).isNull) Expr.splice(isOption.value.empty)
            else Expr.splice(isOption.value.of(read(r, s)))
          })
        }
      case _ => MIO.pure(Left("not an Option"))
    }

  // ------------------------------------------------------------------------------------------------ lists and maps

  private def writeCollection[B: Type](shared: Shared, w: Expr[ShapeWriter], s: Expr[Schema], v: Expr[B]): Attempt[Expr[Unit]] = {
    implicit val UnitT: Type[Unit]     = Types.Unit
    implicit val SchemaT: Type[Schema] = Types.Schema
    implicit val StringT: Type[String] = Types.String
    Type[B] match {
      case IsCollection(isCollection) =>
        import isCollection.Underlying as Item
        isCollection.value.asMap match {
          case Some(isMap) =>
            import isMap.{Key, Value}
            if (!(Type[Key] =:= Types.String))
              MIO.fail(DerivationError.UnsupportedMapKey(Type.plainPrint[B], Type.plainPrint[Key]))
            else
              writer[Value](shared).map { write =>
                val pairs = isCollection.value.asIterable(v)
                Right(Expr.quote {
                  val all = Expr.splice(pairs)
                  val m   = Expr.splice(s).members(1)
                  Expr.splice(w).writeMap(Expr.splice(s), all.size) { mw =>
                    all.foreach { pair =>
                      mw.writeEntry(Expr.splice(isMap.key(Expr.quote(pair)).upcast[String])) { vw =>
                        Expr.splice(write(Expr.quote(vw), Expr.quote(m), isMap.value(Expr.quote(pair))))
                      }
                    }
                  }
                })
              }
          case None =>
            writer[Item](shared).map { write =>
              val items = isCollection.value.asIterable(v)
              Right(Expr.quote {
                val all = Expr.splice(items)
                val m   = Expr.splice(s).members(0)
                Expr.splice(w).writeList(Expr.splice(s), all.size) { lw =>
                  all.foreach(item => Expr.splice(write(Expr.quote(lw), Expr.quote(m), Expr.quote(item))))
                }
              })
            }
        }
      case _ => MIO.pure(Left("not a collection"))
    }
  }

  /** What a null element of a list or map becomes: `None` where the element type is an Option, else it is skipped. */
  private def nullElement[X: Type]: Option[Expr[X]] = Type[X] match {
    case IsOption(isOption) => Some(isOption.value.empty)
    case _                  => None
  }

  private def readCollection[B: Type](shared: Shared, r: Expr[ShapeReader], s: Expr[Schema]): Attempt[Expr[B]] = {
    implicit val SchemaT: Type[Schema] = Types.Schema
    implicit val StringT: Type[String] = Types.String
    implicit val UnitT: Type[Unit]     = Types.Unit
    Type[B] match {
      case IsCollection(isCollection) =>
        import isCollection.Underlying as Item
        import isCollection.value.CtorResult
        if (!(Type[CtorResult] <:< Type[B]))
          MIO.fail(DerivationError.UnsupportedCollection(Type.plainPrint[B], Type.plainPrint[CtorResult]))
        else {
          val factory = isCollection.value.factory
          isCollection.value.asMap match {
            case Some(isMap) =>
              import isMap.{Key, Value}
              if (!(Type[Key] =:= Types.String))
                MIO.fail(DerivationError.UnsupportedMapKey(Type.plainPrint[B], Type.plainPrint[Key]))
              else
                reader[Value](shared).map { read =>
                  def entry(k: Expr[String], value: Expr[Value]): Expr[Item] =
                    isMap.pair(k.asInstanceOf[Expr[Key]], value)
                  Right(Expr.quote {
                    val b = Expr.splice(factory).newBuilder
                    val m = Expr.splice(s).members(1)
                    Expr.splice(r).readMap(Expr.splice(s)) { (k, vr) =>
                      if (!vr.isNull) b.addOne(Expr.splice(entry(Expr.quote(k), read(Expr.quote(vr), Expr.quote(m))))): Unit
                      else Expr.splice(nullElement[Value].fold(Expr.quote(())) { none =>
                        Expr.quote(b.addOne(Expr.splice(entry(Expr.quote(k), none))): Unit)
                      })
                    }
                    b.result().asInstanceOf[B]
                  })
                }
            case None =>
              reader[Item](shared).map { read =>
                Right(Expr.quote {
                  val b = Expr.splice(factory).newBuilder
                  val m = Expr.splice(s).members(0)
                  Expr.splice(r).readList(Expr.splice(s)) { er =>
                    if (!er.isNull) b.addOne(Expr.splice(read(Expr.quote(er), Expr.quote(m)))): Unit
                    else Expr.splice(nullElement[Item].fold(Expr.quote(()))(none => Expr.quote(b.addOne(Expr.splice(none)): Unit)))
                  }
                  b.result().asInstanceOf[B]
                })
              }
          }
        }
      case _ => MIO.pure(Left("not a collection"))
    }
  }

  // ------------------------------------------------------------------------------------------------ structures

  private def writeStructure[B: Type](shared: Shared, w: Expr[ShapeWriter], s: Expr[Schema], v: Expr[B]): Attempt[Expr[Unit]] = {
    implicit val UnitT: Type[Unit]                   = Types.Unit
    implicit val MembersT: Type[IndexedSeq[Schema]]  = Types.Members
    CaseClass.parse[B].toEither match {
      case Left(reason)                                  => MIO.pure(Left(reason))
      case Right(_) if Type.isObject[B] || Type.isVal[B] => MIO.pure(Left("a singleton"))
      case Right(caseClass) =>
        // One statement per field, in member order; fields are derived in parallel so every failure is reported.
        caseClass
          .caseFieldValuesAt(v)
          .toList
          .zipWithIndex
          .parTraverse { case ((name, field), index) =>
            import field.{Underlying as F, value as fieldValue}
            Log.namedScope(s"field $name: ${Type.prettyPrint[F]}")(fieldWriter[F](shared, fieldValue, index))
          }
          .map { statements =>
            Right(Expr.quote {
              Expr.splice(w).writeStruct(Expr.splice(s)) { sw =>
                val ms = Expr.splice(s).members
                Expr.splice(statements.foldLeft(Expr.quote(())) { (acc, statement) =>
                  Expr.quote { Expr.splice(acc); Expr.splice(statement(Expr.quote(sw), Expr.quote(ms))) }
                })
              }
            })
          }
    }
  }

  /** A structure member: an empty Option is left out (an empty `@idempotencyToken` String gets a fresh token). */
  private def fieldWriter[F: Type](shared: Shared, value: Expr[F], index: Int)
      : MIO[(Expr[ShapeWriter], Expr[IndexedSeq[Schema]]) => Expr[Unit]] = {
    implicit val UnitT: Type[Unit]     = Types.Unit
    implicit val SchemaT: Type[Schema] = Types.Schema
    def member(ms: Expr[IndexedSeq[Schema]]): Expr[Schema] = Expr.quote(Expr.splice(ms)(Expr.splice(Expr(index))))
    Type[F] match {
      case IsOption(isOption) =>
        import isOption.Underlying as Item
        writer[Item](shared).map { write => (sw, ms) =>
          val ifEmpty =
            if (Type[Item] =:= Types.String)
              Expr.quote {
                if (Expr.splice(member(ms)).hasTrait(smithy.scala.runtime.Traits.IdempotencyToken))
                  Expr.splice(sw).writeString(Expr.splice(member(ms)), smithy.scala.hearth.HearthCodec.idempotencyToken())
              }
            else Expr.quote(())
          isOption.value.fold[Unit](value)(onEmpty = ifEmpty, onSome = item => write(sw, member(ms), item))
        }
      case _ =>
        writer[F](shared).map(write => (sw, ms) => write(sw, member(ms), value))
    }
  }

  /** One field of a structure being read: its reader, and its value when the input leaves it out. */
  private final class FieldRead[F](val param: Parameter, val index: Int, val read: ReadCall[F], val missing: Expr[Schema] => Expr[F])(
      using val tpe: Type[F]
  )

  /** What one field contributes to the generated block: its assignment inside `readStruct`, and its constructor argument. */
  private final case class FieldSlot(name: String, assign: (Expr[Schema], Expr[ShapeReader]) => Expr[Unit], arg: Expr_??)

  private def readStructure[B: Type](shared: Shared, r: Expr[ShapeReader], s: Expr[Schema]): Attempt[Expr[B]] = {
    implicit val SchemaT: Type[Schema]   = Types.Schema
    implicit val BooleanT: Type[Boolean] = Types.Boolean
    implicit val UnitT: Type[Unit]       = Types.Unit
    CaseClass.parse[B].toEither match {
      case Left(reason)                                  => MIO.pure(Left(reason))
      case Right(_) if Type.isObject[B] || Type.isVal[B] => MIO.pure(Left("a singleton"))
      case Right(caseClass) =>
        val params = caseClass.primaryConstructor.totalParameters.flatten.toList.map(_._2).zipWithIndex
        params
          .parTraverse { case (param, index) =>
            import param.tpe.Underlying as F
            Log.namedScope(s"field ${param.name}: ${Type.prettyPrint[F]}") {
              for {
                read    <- reader[F](shared)
                missing <- missingValue[F](shared, param)
              } yield new FieldRead[F](param, index, read, missing): FieldRead[?]
            }
          }
          .flatMap { fields =>
            // var value_i: F_i; var seen_i = false, then one readStruct that assigns them, then the constructor call
            // with `if (seen_i) value_i else <missing>` for each field.
            def slot[F](f: FieldRead[F]): ValDefs[FieldSlot] = {
              implicit val FieldT: Type[F] = f.tpe
              ValDefs.directStyle.scoped { runSafe =>
                val (value, setValue) = runSafe(ValDefs.createVar[F](Expr.quote(null.asInstanceOf[F])))
                val (seen, setSeen)   = runSafe(ValDefs.createVar[Boolean](Expr.quote(false)))
                val member            = Expr.quote(Expr.splice(s).members(Expr.splice(Expr(f.index))))
                FieldSlot(
                  f.param.name,
                  (m, mr) => Expr.quote { Expr.splice(setValue(f.read(mr, m))); Expr.splice(setSeen(Expr.quote(true))) },
                  Expr.quote(if (Expr.splice(seen)) Expr.splice(value) else Expr.splice(f.missing(member))).as_??
                )
              }
            }
            MIO.scoped { runSafe =>
              Right(fields.traverse(f => slot(f)).use { slots =>
                val byName = slots.map(slot => slot.name -> slot).toMap
                val construct = runSafe(caseClass.parConstruct[MIO](param => MIO.pure(byName(param.name).arg)))
                  .getOrElse(runSafe(MIO.fail(DerivationError.NotConstructible(Type.plainPrint[B]))))
                def assignments(m: Expr[Schema], mr: Expr[ShapeReader]): Expr[Unit] =
                  slots.zipWithIndex.foldRight(Expr.quote(())) { case ((slot, index), orElse) =>
                    Expr.quote {
                      if (Expr.splice(m).memberIndex == Expr.splice(Expr(index))) Expr.splice(slot.assign(m, mr))
                      else Expr.splice(orElse)
                    }
                  }
                Expr.quote {
                  Expr.splice(r).readStruct(Expr.splice(s)) { (m, mr) =>
                    if (!mr.isNull) Expr.splice(assignments(Expr.quote(m), Expr.quote(mr)))
                  }
                  Expr.splice(construct)
                }
              })
            }
          }
    }
  }

  /**
    * A member the input leaves out: its default value; `None` for an Option; an empty collection; a simple type's
    * zero (Smithy's error correction); a provided codec's own `absent`; else a SerializationException naming it.
    */
  private def missingValue[F: Type](shared: Shared, param: Parameter): MIO[Expr[Schema] => Expr[F]] = {
    val default: Option[Expr[F]] =
      if (!param.hasDefault) None
      else
        param.defaultValue.flatMap { method =>
          type EitherString[X] = Either[String, X]
          method
            .foldF[EitherString](
              onInstance = _ => Left("a default value needs no instance"),
              onTypes = _ => Right(Map.empty),
              onValues = _ => Right(Map.empty)
            )
            .flatten
            .toOption
            .map { ee =>
              import ee.Underlying
              ee.value.upcast[F]
            }
        }
    def fail(member: Expr[Schema]): Expr[F] = Expr.quote(smithy.scala.hearth.HearthCodec.missing(Expr.splice(member)))
    default match {
      case Some(d) => MIO.pure(_ => d)
      case None =>
        (Type[F], simple[F]) match {
          case (IsOption(isOption), _) => MIO.pure(_ => isOption.value.empty)
          case (_, Some(simpleType))   => MIO.pure(_ => simpleType.zero)
          case (IsCollection(isCollection), _) =>
            import isCollection.Underlying as Item
            import isCollection.value.CtorResult
            val factory = isCollection.value.factory
            MIO.pure(_ => Expr.quote(Expr.splice(factory).newBuilder.result().asInstanceOf[F]))
          case _ =>
            provided[F](shared).map {
              case Some(codec) => member => Expr.quote(Expr.splice(codec).absent.getOrElse(Expr.splice(fail(member))))
              case None        => fail
            }
        }
    }
  }

  // ------------------------------------------------------------------------------------------------ unions

  private val UnknownMemberCase = "UnknownMember"

  /**
    * A union is a Scala 3 enum: one case per member, in member order, each a singleton (a `Unit` member) or a case class
    * with one field, then `UnknownMember(name)`. Case `i` is member `i` (Scala 3 enums list cases in declaration order).
    */
  private def unionCases[B: Type]: Either[String, List[(String, ??<:[B])]] =
    Enum.parse[B].toEither.map(_.directChildren.toList)

  private def caseIndex[B: Type, C: Type](cases: List[(String, ??<:[B])]): Int =
    cases.indexWhere { case (_, child) => import child.Underlying as X; Type[X] =:= Type[C] }

  private def writeUnion[B: Type](shared: Shared, w: Expr[ShapeWriter], s: Expr[Schema], v: Expr[B]): Attempt[Expr[Unit]] = {
    implicit val UnitT: Type[Unit]     = Types.Unit
    implicit val SchemaT: Type[Schema] = Types.Schema
    (unionCases[B], Enum.parse[B].toEither) match {
      case (Right(cases), Right(enumm)) =>
        enumm
          .parMatchOn[MIO, Unit](v) { matched =>
            import matched.{Underlying as C, value as caseValue}
            val index = caseIndex[B, C](cases)
            val name  = cases(index)._1
            def member = Expr.quote(Expr.splice(s).members(Expr.splice(Expr(index))))
            if (name == UnknownMemberCase)
              MIO.pure(Expr.quote(smithy.scala.hearth.HearthCodec.unknownUnionMember(Expr.splice(s))))
            else if (Expr.singletonOf[C].isDefined)
              MIO.pure(Expr.quote(Expr.splice(w).writeStruct(Expr.splice(s))(sw => sw.writeStruct(Expr.splice(member))(_ => ()))))
            else
              CaseClass.parse[C].toOption.map(_.caseFieldValuesAt(caseValue).toList) match {
                case Some(List((_, field))) =>
                  import field.{Underlying as F, value as fieldValue}
                  writer[F](shared).map { write =>
                    Expr.quote(Expr.splice(w).writeStruct(Expr.splice(s)) { sw =>
                      Expr.splice(write(Expr.quote(sw), member, fieldValue))
                    })
                  }
                case _ => MIO.fail(DerivationError.UnsupportedUnionCase(Type.plainPrint[B], name))
              }
          }
          .map(_.toRight("an enum without cases"))
      case (Left(reason), _) => MIO.pure(Left(reason))
      case (_, Left(reason)) => MIO.pure(Left(reason))
    }
  }

  private def readUnion[B: Type](shared: Shared, r: Expr[ShapeReader], s: Expr[Schema]): Attempt[Expr[B]] = {
    implicit val UnitT: Type[Unit]       = Types.Unit
    implicit val BooleanT: Type[Boolean] = Types.Boolean
    implicit val StringT: Type[String]   = Types.String
    implicit val SchemaT: Type[Schema]   = Types.Schema
    unionCases[B] match {
      case Left(reason) => MIO.pure(Left(reason))
      case Right(cases) =>
        val unknown = cases.find(_._1 == UnknownMemberCase)
        // One builder per known case: given the member's reader, the value of that case.
        cases.zipWithIndex
          .filterNot(_._1._1 == UnknownMemberCase)
          .parTraverse { case ((name, child), index) =>
            import child.Underlying as C
            // the builders return MIO and are run by the block that splices them (a runSafe is only valid
            // inside the MIO.scoped that created it)
            Expr.singletonOf[C] match {
              case Some(singleton) =>
                MIO.pure((index, (_: Expr[ShapeReader], _: Expr[Schema]) => MIO.pure(singleton.upcast[B])))
              case None =>
                CaseClass.parse[C].toOption.map(cc => (cc, cc.primaryConstructor.totalParameters.flatten.toList)) match {
                  case Some((cc, List((_, param)))) =>
                    import param.tpe.Underlying as F
                    reader[F](shared).map { read =>
                      (index, (mr: Expr[ShapeReader], m: Expr[Schema]) =>
                        cc.parConstruct[MIO](_ => MIO.pure(read(mr, m).as_??)).flatMap {
                          case Some(built) => MIO.pure(built.upcast[B])
                          case None        => MIO.fail(DerivationError.NotConstructible(Type.plainPrint[C]))
                        })
                    }
                  case _ => MIO.fail(DerivationError.UnsupportedUnionCase(Type.plainPrint[B], name))
                }
            }
          }
          .flatMap { builders =>
            MIO.scoped { runSafe =>
              def unknownValue(name: Expr[String]): Expr[B] = unknown match {
                case Some((_, child)) =>
                  import child.Underlying as C
                  CaseClass.parse[C].toOption
                    .flatMap(cc => runSafe(cc.parConstruct[MIO](_ => MIO.pure(name.as_??))))
                    .map(_.upcast[B])
                    .getOrElse(runSafe(MIO.fail(DerivationError.UnsupportedUnionCase(Type.plainPrint[B], UnknownMemberCase))))
                case None => Expr.quote(smithy.scala.hearth.HearthCodec.missing(Expr.splice(s)))
              }
              Right(ValDefs.directStyle.scoped { runVals =>
                val (result, setResult) = runVals(ValDefs.createVar[B](Expr.quote(null.asInstanceOf[B])))
                val (seen, setSeen)     = runVals(ValDefs.createVar[Boolean](Expr.quote(false)))
                val (name, setName)     = runVals(ValDefs.createVar[String](Expr.quote("")))
                (result, setResult, seen, setSeen, name, setName)
              }.use { case (result, setResult, seen, setSeen, name, setName) =>
                def assignments(m: Expr[Schema], mr: Expr[ShapeReader]): Expr[Unit] =
                  builders.foldRight(Expr.quote(())) { case ((index, build), orElse) =>
                    Expr.quote {
                      if (Expr.splice(m).memberIndex == Expr.splice(Expr(index))) {
                        Expr.splice(setResult(runSafe(build(mr, m))))
                        Expr.splice(setSeen(Expr.quote(true)))
                      } else Expr.splice(orElse)
                    }
                  }
                Expr.quote {
                  Expr.splice(r).readStruct(Expr.splice(s), (n: String) => Expr.splice(setName(Expr.quote(n)))) { (m, mr) =>
                    if (!mr.isNull) Expr.splice(assignments(Expr.quote(m), Expr.quote(mr)))
                  }
                  if (Expr.splice(seen)) Expr.splice(result) else Expr.splice(unknownValue(name))
                }
              })
            }
          }
    }
  }

  /** What a codec says for a missing required member of its type: a union's unknown case, else nothing. */
  private def absentOf[A: Type]: MIO[Expr[Option[A]]] = {
    implicit val OptionA: Type[Option[A]] = Types.Option[A]
    implicit val StringT: Type[String]    = Types.String
    val unknown = for {
      cases      <- unionCases[A].toOption
      (_, child) <- cases.find(_._1 == UnknownMemberCase)
    } yield {
      import child.Underlying as C
      CaseClass.parse[C].toOption.flatMap { cc =>
        cc.parConstruct[MIO](_ => MIO.pure(Expr("").as_??)).unsafe.runSync._2.toOption.flatten.map(_.upcast[A])
      }
    }
    MIO.pure(unknown.flatten match {
      case Some(value) => Expr.quote(Some(Expr.splice(value)))
      case None        => Expr.quote(None)
    })
  }
}
