package smithyscala.runtime

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

final case class Credentials(accessKeyId: String, secretAccessKey: String, sessionToken: Option[String] = None) {
  override def toString: String = s"Credentials($accessKeyId, ****)"
}

/** The identity resolver: called once per attempt, so it can refresh. */
trait CredentialsProvider {
  def resolve(): Credentials
}

object CredentialsProvider {
  def static(credentials: Credentials): CredentialsProvider = () => credentials

  /** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and optional `AWS_SESSION_TOKEN`. */
  val environment: CredentialsProvider = () => {
    val env = sys.env
    (env.get("AWS_ACCESS_KEY_ID"), env.get("AWS_SECRET_ACCESS_KEY")) match {
      case (Some(id), Some(secret)) => Credentials(id, secret, env.get("AWS_SESSION_TOKEN"))
      case _ => throw new IllegalStateException("AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are not set")
    }
  }
}

/**
  * AWS Signature Version 4 for a request body held in memory. It signs `host`, every header the
  * request carries, and `x-amz-date` (plus `x-amz-security-token` for session credentials).
  */
object SigV4 {
  private val AmzDate   = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
  private val ShortDate = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

  // Headers that proxies or the HTTP client may change, so they're never signed.
  private val unsigned = Set("authorization", "user-agent", "x-amzn-trace-id", "expect", "transfer-encoding", "connection")

  def sign(request: HttpRequest, credentials: Credentials, region: String, service: String, now: Instant): HttpRequest = {
    val amzDate = AmzDate.format(now)
    val date    = ShortDate.format(now)

    val withDate = request.withHeader("X-Amz-Date", amzDate)
    val toSign   = credentials.sessionToken.fold(withDate)(t => withDate.withHeader("X-Amz-Security-Token", t))

    val headers = (("host" -> hostHeader(request.uri)) +: toSign.headers.entries
      .map((k, v) => k.toLowerCase -> v.trim.replaceAll("\\s+", " "))
      .filterNot((k, _) => k == "host" || unsigned.contains(k)))
      .groupMap(_._1)(_._2)
      .toVector
      .sortBy(_._1)
    val signedHeaders    = headers.map(_._1).mkString(";")
    val canonicalHeaders = headers.map((k, vs) => s"$k:${vs.mkString(",")}\n").mkString

    val canonicalRequest = Seq(
      request.method,
      canonicalPath(request.uri),
      canonicalQuery(request.uri),
      canonicalHeaders,
      signedHeaders,
      hex(sha256(request.body))
    ).mkString("\n")

    val scope        = s"$date/$region/$service/aws4_request"
    val stringToSign = Seq("AWS4-HMAC-SHA256", amzDate, scope, hex(sha256(canonicalRequest.getBytes(UTF_8)))).mkString("\n")

    val key = Seq(date, region, service, "aws4_request")
      .foldLeft(("AWS4" + credentials.secretAccessKey).getBytes(UTF_8))((k, part) => hmac(k, part))
    val signature = hex(hmac(key, stringToSign))

    toSign.withHeader(
      "Authorization",
      s"AWS4-HMAC-SHA256 Credential=${credentials.accessKeyId}/$scope, SignedHeaders=$signedHeaders, Signature=$signature"
    )
  }

  /** The Host header the HTTP client will send: the port only when it isn't the scheme's default. */
  private def hostHeader(uri: URI): String = {
    val port        = uri.getPort
    val defaultPort = if (uri.getScheme.equalsIgnoreCase("https")) 443 else 80
    if (port == -1 || port == defaultPort) uri.getHost else s"${uri.getHost}:$port"
  }

  // Services other than S3 encode the (already encoded) path a second time.
  private def canonicalPath(uri: URI): String = {
    val raw = Option(uri.getRawPath).filter(_.nonEmpty).getOrElse("/")
    raw.split("/", -1).map(encode).mkString("/")
  }

  private def canonicalQuery(uri: URI): String =
    Option(uri.getRawQuery).filter(_.nonEmpty).fold("") { q =>
      q.split("&").toVector
        .map { p =>
          val i = p.indexOf('=')
          val (k, v) = if (i < 0) (p, "") else (p.substring(0, i), p.substring(i + 1))
          encode(URLDecoder.decode(k, UTF_8)) -> encode(URLDecoder.decode(v, UTF_8))
        }
        .sorted
        .map((k, v) => s"$k=$v")
        .mkString("&")
    }

  /** RFC 3986 encoding: everything except unreserved characters. */
  private def encode(s: String): String =
    URLEncoder.encode(s, UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

  private def sha256(bytes: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-256").digest(bytes)

  private def hmac(key: Array[Byte], data: String): Array[Byte] = {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(new SecretKeySpec(key, "HmacSHA256"))
    mac.doFinal(data.getBytes(UTF_8))
  }

  private def hex(bytes: Array[Byte]): String = bytes.map(b => f"${b & 0xff}%02x").mkString
}
