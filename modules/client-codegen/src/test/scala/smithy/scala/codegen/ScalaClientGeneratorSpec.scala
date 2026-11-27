package smithy.scala.codegen

import java.nio.file.Files

import scala.jdk.CollectionConverters.*

import software.amazon.smithy.build.SmithyBuild
import software.amazon.smithy.build.model.SmithyBuildConfig
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId

class ScalaClientGeneratorSpec extends munit.FunSuite {

  private val idl =
    """$version: "2"
      |namespace example.weather
      |
      |use smithy.protocols#rpcv2Json
      |
      |@rpcv2Json
      |service Weather {
      |    version: "2026-11-26"
      |    operations: [GetForecast, Ping]
      |}
      |
      |@paginated(inputToken: "token", outputToken: "next", items: "days", pageSize: "size")
      |operation GetForecast {
      |    input := {
      |        @required
      |        city: String
      |        size: Integer
      |        token: String
      |        units: Units = "METRIC"
      |        detail: Detail = 1
      |    }
      |    output := {
      |        days: Days
      |        next: String
      |        @required
      |        type: Kind
      |    }
      |    errors: [Busy]
      |}
      |
      |operation Ping {}
      |
      |enum Units {
      |    METRIC
      |    IMPERIAL
      |}
      |
      |intEnum Detail {
      |    LOW = 1
      |    HIGH = 2
      |}
      |
      |@sparse
      |list Days { member: Day }
      |
      |structure Day {
      |    @timestampFormat("date-time")
      |    date: Timestamp
      |    copy: String
      |}
      |
      |union Kind {
      |    sunny: Unit
      |    rain: Integer
      |}
      |
      |@error("server")
      |@retryable(throttling: true)
      |structure Busy {
      |    message: String
      |}
      |""".stripMargin

  private lazy val model: Model =
    Model.assembler().discoverModels().addUnparsedModel("weather.smithy", idl).assemble().unwrap()

  private lazy val files: Map[String, String] =
    ScalaClientGenerator.generate(model, ShapeId.from("example.weather#Weather"), "gen.weather")

  private def src(name: String): String = files.getOrElse(s"gen/weather/$name.scala", fail(s"no $name.scala in ${files.keys}"))

  test("one file per shape, plus the client") {
    assertEquals(
      files.keySet.map(_.stripPrefix("gen/weather/")),
      Set("Busy", "Day", "Days", "Detail", "GetForecastInput", "GetForecastOutput", "Kind", "Units", "WeatherClient")
        .map(_ + ".scala")
    )
  }

  test("required, optional and default members") {
    val s = src("GetForecastInput")
    assert(s.contains("city: java.lang.String,"), s)
    assert(s.contains("size: scala.Option[scala.Int] = scala.None"), s)
    assert(s.contains("units: Units = Units.fromValue(\"METRIC\")"), s)
    assert(s.contains("detail: Detail = Detail.fromValue(1)"), s)
  }

  test("names that clash with Scala keywords or case class members are escaped") {
    assert(src("GetForecastOutput").contains("`type`: Kind"))
    assert(src("Day").contains("copy_ : scala.Option[java.lang.String]"))
  }

  test("runtime traits are carried in the schema") {
    // rpcv2Json's @protocolDefinition doesn't list timestampFormat: it always uses epoch seconds.
    assert(!src("Day").contains("timestampFormat"), src("Day"))
    assert(src("Days").contains("rt.Traits(\"smithy.api#sparse\" -> \"\")"))
    assert(src("Busy").contains("\"smithy.api#retryable\" -> \"throttling\""))
  }

  test("sparse lists hold options; unions get Unit cases and an unknown case") {
    assert(src("Days").contains("type Days = scala.collection.immutable.List[scala.Option[Day]]"))
    val k = src("Kind")
    assert(k.contains("  case Sunny\n"), k)
    assert(k.contains("  case Rain(value: scala.Int)"), k)
    assert(k.contains("case UnknownMember(name: java.lang.String)"), k)
  }

  test("errors extend ServiceException and say whether they're retryable") {
    val b = src("Busy")
    assert(b.contains("extends rt.ServiceException(rt.ShapeId(\"example.weather\", \"Busy\"), message.orNull, \"server\")"), b)
    assert(b.contains("override def smithyThrottling: scala.Boolean = true"), b)
  }

  test("the client: protocol, Unit input, paginator") {
    val c = src("WeatherClient")
    assert(c.contains("rt.ProtocolInfo.RpcV2Json"), c)
    assert(c.contains("def ping(): scala.Unit"), c)
    assert(c.contains("def getForecastPaginator(input: GetForecastInput)"), c)
    assert(c.contains("in.copy(token = scala.Some(t))"), c)
  }

  test("codecs are derived, not generated") {
    assert(src("GetForecastInput").contains("given codec: rt.SchemaCodec[GetForecastInput] = smithy.scala.hearth.HearthCodec.derived[GetForecastInput](schema)"))
    assert(src("Kind").contains("given codec: rt.SchemaCodec[Kind] = smithy.scala.hearth.HearthCodec.derived[Kind](schema)"))
    assert(src("Units").contains("lazy val knownValues: scala.collection.immutable.List[Units] = smithy.scala.hearth.HearthCodec.enumValues[Units]"))
    assert(src("Detail").contains("rt.ShapeCodec.intEnum(fromValue, _.value)"))
    assert(!src("Days").contains("def write"))
    assert(src("WeatherClient").contains("rt.TypeRegistry.of[Busy *: EmptyTuple]"))
  }

  test("derivation = mirror: the given codecs and enum values call the runtime's Mirror derivation instead") {
    val mirror = ScalaClientGenerator.generate(model, ShapeId.from("example.weather#Weather"), "gen.weather", Derivation.Mirror)
    def file(n: String) = mirror(s"gen/weather/$n.scala")
    assert(file("GetForecastInput").contains("= rt.ShapeCodec.struct[GetForecastInput](schema)"))
    assert(file("Kind").contains("= rt.ShapeCodec.union[Kind](schema)"))
    assert(file("Units").contains("= rt.ShapeCodec.enumValues[Units]"))
    assert(!file("GetForecastInput").contains("HearthCodec"))
  }

  test("services without a supported protocol are rejected with the reason") {
    val restJson = Model.assembler().discoverModels()
      .addUnparsedModel("r.smithy", "$version: \"2\"\nnamespace x\nuse aws.protocols#restJson1\n@restJson1\nservice S {}\n")
      .assemble().unwrap()
    val e = intercept[UnsupportedOperationException](ScalaClientGenerator.generate(restJson, ShapeId.from("x#S"), "x"))
    assert(e.getMessage.contains("aws.protocols#restJson1"), e.getMessage)
  }

  test("runs as a smithy-build plugin") {
    val out = Files.createTempDirectory("scala-client-codegen")
    val config = SmithyBuildConfig.builder()
      .version("1.0")
      .outputDirectory(out.toString)
      .plugins(Map[String, software.amazon.smithy.model.node.ObjectNode](
        "scala-client-codegen" -> Node.objectNode()
          .withMember("service", "example.weather#Weather")
          .withMember("package", "gen.weather")
      ).asJava)
      .build()
    val result = new SmithyBuild().config(config).model(model).build()
    val written = result.allArtifacts().iterator().asScala.map(_.getFileName.toString).toSet
    assert(written.contains("WeatherClient.scala"), written)
  }

  test("both plugins are registered with ServiceLoader; the server one is a stub") {
    val names = java.util.ServiceLoader.load(classOf[software.amazon.smithy.build.SmithyBuildPlugin], getClass.getClassLoader)
      .iterator().asScala.map(_.getName).toSet
    assert(Set("scala-client-codegen", "scala-server-codegen").subsetOf(names), names)
    val config = SmithyBuildConfig.builder()
      .version("1.0")
      .outputDirectory(Files.createTempDirectory("scala-server-codegen").toString)
      .plugins(Map[String, software.amazon.smithy.model.node.ObjectNode]("scala-server-codegen" -> Node.objectNode()).asJava)
      .build()
    val e = intercept[Throwable](new SmithyBuild().config(config).model(model).build())
    assert(e.getMessage.contains("server generation is not implemented yet"), e.getMessage)
  }

  test("kept traits follow the protocol's @protocolDefinition: awsJson keeps timestampFormat, drops jsonName and xmlName") {
    val awsIdl =
      """$version: "2"
        |namespace example.q
        |
        |use aws.protocols#awsJson1_0
        |
        |@awsJson1_0
        |service Q {
        |    version: "2026-11-27"
        |    operations: [Send]
        |}
        |
        |operation Send {
        |    input := {
        |        @required
        |        @jsonName("queue_url")
        |        @xmlName("Url")
        |        QueueUrl: String
        |        @timestampFormat("date-time")
        |        At: Timestamp
        |    }
        |    errors: [Gone]
        |}
        |
        |@error("client")
        |@httpError(404)
        |structure Gone {
        |    message: String
        |}
        |""".stripMargin
    val awsModel = Model.assembler().discoverModels().addUnparsedModel("q.smithy", awsIdl).assemble().unwrap()
    val out      = ScalaClientGenerator.generate(awsModel, ShapeId.from("example.q#Q"), "gen.q")
    val input    = out("gen/q/SendInput.scala")
    assert(input.contains("rt.MemberDef(\"QueueUrl\", () => rt.Prelude.String, rt.Traits(\"smithy.api#required\" -> \"\"))"), input)
    assert(input.contains("rt.Traits(\"smithy.api#timestampFormat\" -> \"date-time\")"), input)
    assert(!input.contains("jsonName") && !input.contains("xmlName"), input)
    // httpError isn't in awsJson's list; error is a core trait.
    val gone = out("gen/q/Gone.scala")
    assert(gone.contains("\"smithy.api#error\" -> \"client\"") && !gone.contains("httpError"), gone)
  }
}
