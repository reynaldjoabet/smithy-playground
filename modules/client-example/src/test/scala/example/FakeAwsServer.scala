package example

import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters.*

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import com.sun.net.httpserver.HttpServer

/** A local HTTP server that records requests and answers with canned responses, in order. */
final class FakeAwsServer extends AutoCloseable {
  final case class Recorded(method: String, path: String, headers: Map[String, String], body: String) {
    def json: JsonNode = FakeAwsServer.mapper.readTree(body)
  }
  final case class Reply(status: Int, body: String, headers: Map[String, String] = Map.empty)

  private val replies  = new ConcurrentLinkedQueue[Reply]()
  private val recorded = new ConcurrentLinkedQueue[Recorded]()
  private val server   = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)

  server.createContext(
    "/",
    exchange => {
      val headers = exchange.getRequestHeaders.asScala.map((k, v) => k.toLowerCase -> v.asScala.mkString(",")).toMap
      val body    = new String(exchange.getRequestBody.readAllBytes(), UTF_8)
      recorded.add(Recorded(exchange.getRequestMethod, exchange.getRequestURI.getPath, headers, body)): Unit
      val reply = Option(replies.poll()).getOrElse(Reply(500, """{"__type":"NoReplyQueued"}"""))
      val bytes = reply.body.getBytes(UTF_8)
      exchange.getResponseHeaders.add("Content-Type", "application/x-amz-json-1.0")
      reply.headers.foreach((k, v) => exchange.getResponseHeaders.add(k, v))
      exchange.sendResponseHeaders(reply.status, if (bytes.isEmpty) -1L else bytes.length.toLong)
      if (bytes.nonEmpty) exchange.getResponseBody.write(bytes)
      exchange.close()
    }
  )
  server.start()

  val endpoint: URI = URI.create(s"http://127.0.0.1:${server.getAddress.getPort}")

  def reply(status: Int, body: String, headers: (String, String)*): Unit =
    replies.add(Reply(status, body, headers.toMap)): Unit

  def requests: List[Recorded] = recorded.asScala.toList

  def close(): Unit = server.stop(0)
}

object FakeAwsServer {
  val mapper: JsonMapper = JsonMapper.builder().build()
  def json(s: String): JsonNode = mapper.readTree(s)
}
