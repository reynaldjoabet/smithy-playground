package smithyscala.runtime

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest as JRequest
import java.net.http.HttpResponse as JResponse
import java.time.Duration
import javax.net.ssl.SSLContext

import scala.jdk.CollectionConverters.*

/** Header names compare case-insensitively; order is kept. Entries are named tuples. */
final case class Headers(entries: Vector[(name: String, value: String)]) {
  def get(name: String): Option[String] = entries.find(_.name.equalsIgnoreCase(name)).map(_.value)
  def set(name: String, value: String): Headers =
    Headers(entries.filterNot(_.name.equalsIgnoreCase(name)).appended((name = name, value = value)))
  override def toString: String = entries.map(e => s"${e.name}: ${e.value}").mkString("Headers(", ", ", ")")
}

object Headers {
  val empty: Headers = Headers(Vector.empty)
  def apply(pairs: (String, String)*): Headers = Headers(pairs.toVector.map((n, v) => (name = n, value = v)))
}

final case class HttpRequest(method: String, uri: URI, headers: Headers, body: Array[Byte]) {
  def withHeader(name: String, value: String): HttpRequest = copy(headers = headers.set(name, value))
}

/** `protocol` is the HTTP version the transport actually used, such as `HTTP/1.1` or `HTTP/2`. */
final case class HttpResponse(status: Int, headers: Headers, body: Array[Byte], protocol: String = "HTTP/1.1")

/** The transport sends bytes and returns bytes. It knows nothing about Smithy. */
trait HttpTransport {
  def send(request: HttpRequest): HttpResponse
}

/** A network failure: nothing came back, so the request may be retried. */
final class TransportException(message: String, cause: Throwable) extends RuntimeException(message, cause)

/** The JDK's `java.net.http` client. */
final class JavaHttpTransport(client: HttpClient, timeout: Duration) extends HttpTransport {

  // java.net.http sets these itself and refuses them from callers.
  private val restricted = Set("host", "content-length", "connection", "expect", "upgrade")

  /**
    * The headers java.net.http adds to every request with a body, in the order it sends them:
    * `Content-Length` and `Host`. Interceptors never see these; loggers can use this to show the
    * request as it goes on the wire.
    */
  def addedHeaders(request: HttpRequest): Headers = JavaHttpTransport.addedHeaders(request)

  def send(request: HttpRequest): HttpResponse = {
    val builder = JRequest
      .newBuilder(request.uri)
      .timeout(timeout)
      .method(request.method, JRequest.BodyPublishers.ofByteArray(request.body))
    for (h <- request.headers.entries if !restricted.contains(h.name.toLowerCase)) builder.header(h.name, h.value): Unit
    val response =
      try client.send(builder.build(), JResponse.BodyHandlers.ofByteArray())
      catch {
        case e: IOException => throw new TransportException(s"${request.method} ${request.uri} failed: $e", e)
      }
    val headers = for {
      (name, values) <- response.headers().map().asScala.toVector
      value          <- values.asScala
    } yield (name = name, value = value)
    val protocol = if (response.version() eq HttpClient.Version.HTTP_2) "HTTP/2" else "HTTP/1.1"
    HttpResponse(response.statusCode(), Headers(headers), response.body(), protocol)
  }
}

object JavaHttpTransport {
  def addedHeaders(request: HttpRequest): Headers = {
    val uri  = request.uri
    val port = uri.getPort
    val defaultPort = if (uri.getScheme.equalsIgnoreCase("https")) 443 else 80
    val host = if (port == -1 || port == defaultPort) uri.getHost else s"${uri.getHost}:$port"
    Headers("Content-Length" -> request.body.length.toString, "Host" -> host)
  }

  lazy val default: JavaHttpTransport = apply()

  def apply(sslContext: Option[SSLContext] = None, timeout: Duration = Duration.ofSeconds(30)): JavaHttpTransport = {
    val b = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
    sslContext.foreach(b.sslContext)
    new JavaHttpTransport(b.build(), timeout)
  }
}
