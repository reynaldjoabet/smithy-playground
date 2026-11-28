package smithy.scala.codegen

import java.nio.file.{Files, Path}

import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait

/**
  * The JSON-RPC protocol definition, from jsonrpclib (resources/META-INF/smithy/jsonrpc.smithy), is on the classpath:
  * models can use `@jsonRpc` without importing it, and its own rules apply.
  */
class JsonRpcSpec extends munit.FunSuite {

  private def model(body: String): Path = {
    val file = Files.createTempFile("jsonrpc", ".smithy")
    Files.writeString(file, "$version: \"2.1\"\nnamespace example.lsp\n\nuse jsonrpc#jsonRpc\nuse jsonrpc#jsonRpcRequest\nuse jsonrpc#jsonRpcNotification\nuse jsonrpc#jsonRpcPayload\n\n" + body)
  }

  private val service =
    """@jsonRpc
      |service Lsp {
      |    operations: [Hover, Exit]
      |}
      |
      |@jsonRpcRequest("textDocument/hover")
      |operation Hover {
      |    input := {
      |        @required
      |        @jsonRpcPayload
      |        params: HoverParams
      |    }
      |    output := {
      |        contents: String
      |    }
      |}
      |
      |@jsonRpcNotification("exit")
      |operation Exit {}
      |
      |structure HoverParams {
      |    @required
      |    uri: String
      |    line: Integer
      |}
      |""".stripMargin

  test("a @jsonRpc service loads strictly, with the protocol and method names") {
    val m     = ModelLoader.load(model(service))
    val proto = m.expectShape(ShapeId.from("jsonrpc#jsonRpc"))
    assert(proto.hasTrait(classOf[ProtocolDefinitionTrait]))
    val listed = proto.expectTrait(classOf[ProtocolDefinitionTrait]).getTraits.toString
    assert(listed.contains("jsonrpc#jsonRpcPayload") && listed.contains("smithy.api#jsonName"), listed)
    assert(!listed.contains("alloy#"), listed)
    val hover = m.expectShape(ShapeId.from("example.lsp#Hover"))
    assertEquals(hover.findTrait("jsonrpc#jsonRpcRequest").get.toNode.expectStringNode.getValue, "textDocument/hover")
  }

  test("an operation can't be both a request and a notification") {
    val both = service.replace("@jsonRpcNotification(\"exit\")", "@jsonRpcNotification(\"exit\")\n@jsonRpcRequest(\"exit\")")
    val e    = intercept[Exception](ModelLoader.load(model(both)))
    assert(e.getMessage.contains("conflicts"), e.getMessage)
  }

  test("@jsonRpcPayload only binds a top-level input, output or error member") {
    val nested = service + "\nstructure Wrapper {\n    @required\n    @jsonRpcPayload\n    inner: HoverParams\n}\n"
    val e      = intercept[Exception](ModelLoader.load(model(nested)))
    assert(e.getMessage.contains("jsonRpcPayload.OnlyTopLevel"), e.getMessage)
  }

  test("the client generator doesn't implement jsonRpc yet, and says so") {
    val e = intercept[UnsupportedOperationException](
      ScalaClientGenerator.generate(ModelLoader.load(model(service)), ShapeId.from("example.lsp#Lsp"), "example.lsp", Derivation.Hearth)
    )
    assert(e.getMessage.contains("jsonrpc#jsonRpc"), e.getMessage)
  }
}
