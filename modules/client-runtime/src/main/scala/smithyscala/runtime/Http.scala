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

final case class HttpResponse(status: Int, headers: Headers, body: Array[Byte])

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
    HttpResponse(response.statusCode(), Headers(headers), response.body())
  }
}

object JavaHttpTransport {
  lazy val default: JavaHttpTransport = apply()

  def apply(sslContext: Option[SSLContext] = None, timeout: Duration = Duration.ofSeconds(30)): JavaHttpTransport = {
    val b = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
    sslContext.foreach(b.sslContext)
    new JavaHttpTransport(b.build(), timeout)
  }
}
