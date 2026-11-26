package smithyscala.runtime

import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

import scala.annotation.tailrec

/** Hooks into each attempt, in the spirit of smithy-java's interceptors. All are optional. */
trait Interceptor {
  def modifyBeforeSigning(request: HttpRequest): HttpRequest  = request
  def modifyBeforeTransmit(request: HttpRequest): HttpRequest = request
  def afterAttempt(request: HttpRequest, result: Either[Throwable, HttpResponse]): Unit = ()
}

/** The standard retry strategy: retry throttling, transient and 5xx errors with full-jitter backoff. */
final case class RetryStrategy(
    maxAttempts: Int = 3,
    baseDelay: Duration = Duration.ofMillis(50),
    maxDelay: Duration = Duration.ofSeconds(20)
) {
  import RetryStrategy.*

  def isRetryable(error: Throwable): Boolean = error match {
    case _: TransportException => true
    case e: ServiceException =>
      val status = e.smithyMetadata.map(_.statusCode).getOrElse(0)
      val code   = e.smithyMetadata.map(_.code).getOrElse(e.smithyShapeId.name)
      e.smithyRetryable || RetryableStatus.contains(status) || ThrottlingCodes.contains(code) ||
      TransientCodes.contains(code)
    case _ => false
  }

  def delayBeforeAttempt(attempt: Int): Duration = {
    val ceiling = math.min(maxDelay.toMillis, baseDelay.toMillis * (1L << math.min(attempt - 1, 20)))
    Duration.ofMillis(ThreadLocalRandom.current().nextLong(ceiling + 1))
  }
}

object RetryStrategy {
  val RetryableStatus: Set[Int] = Set(500, 502, 503, 504)

  val ThrottlingCodes: Set[String] = Set(
    "Throttling", "ThrottlingException", "ThrottledException", "RequestThrottledException",
    "TooManyRequestsException", "ProvisionedThroughputExceededException", "TransactionInProgressException",
    "RequestLimitExceeded", "BandwidthLimitExceeded", "LimitExceededException", "RequestThrottled",
    "SlowDown", "PriorRequestNotComplete", "EC2ThrottledException"
  )

  val TransientCodes: Set[String] = Set("RequestTimeout", "RequestTimeoutException", "InternalError")
}

final case class ClientConfig(
    region: String = sys.env.getOrElse("AWS_REGION", "us-east-1"),
    endpoint: Option[URI] = None,
    credentials: CredentialsProvider = CredentialsProvider.environment,
    transport: HttpTransport = JavaHttpTransport.default,
    retry: RetryStrategy = RetryStrategy(),
    interceptors: List[Interceptor] = Nil,
    clock: () => Instant = () => Instant.now(),
    sleep: Duration => Unit = d => Thread.sleep(d.toMillis)
)

/**
  * Runs one call in the order of the Smithy reference architecture: serialize once, then for each
  * attempt resolve identity, sign, transmit and deserialize, inside the retry loop.
  */
final class ClientPipeline(val service: ServiceInfo, val config: ClientConfig) {
  private val protocol = ClientProtocol.forService(service)

  def invoke[I, O](operation: Operation[I, O], input: I): O = {
    val endpoint = resolveEndpoint()
    val request  = protocol.createRequest(operation, input, endpoint)
    attempt(operation, request, 1)
  }

  /**
    * No endpoint rules engine here: an explicit endpoint wins, otherwise the classic AWS pattern
    * `https://<endpointPrefix>.<region>.amazonaws.com`.
    */
  private def resolveEndpoint(): URI = config.endpoint.getOrElse {
    service.endpointPrefix match {
      case Some(prefix) => URI.create(s"https://$prefix.${config.region}.amazonaws.com")
      case None => throw new IllegalStateException(s"${service.sdkId} has no endpoint prefix; set ClientConfig.endpoint")
    }
  }

  @tailrec
  private def attempt[I, O](operation: Operation[I, O], request: HttpRequest, n: Int): O = {
    val outcome: Either[Throwable, O] =
      try {
        val beforeSigning = config.interceptors.foldLeft(request)((r, i) => i.modifyBeforeSigning(r))
        val signed        = sign(operation, beforeSigning)
        val toSend        = config.interceptors.foldLeft(signed)((r, i) => i.modifyBeforeTransmit(r))
        val response =
          try config.transport.send(toSend)
          catch {
            case e: TransportException =>
              config.interceptors.foreach(_.afterAttempt(toSend, Left(e)))
              throw e
          }
        config.interceptors.foreach(_.afterAttempt(toSend, Right(response)))
        Right(protocol.deserializeResponse(operation, response))
      } catch {
        case e: ServiceException   => Left(e)
        case e: TransportException => Left(e)
      }

    outcome match {
      case Right(output) => output
      case Left(error) if n < config.retry.maxAttempts && config.retry.isRetryable(error) =>
        config.sleep(config.retry.delayBeforeAttempt(n))
        attempt(operation, request, n + 1)
      case Left(error) => throw error
    }
  }

  private def sign[I, O](operation: Operation[I, O], request: HttpRequest): HttpRequest =
    if (operation.authSchemes.contains("aws.auth#sigv4")) {
      val signingName = service.signingName.getOrElse(
        throw new IllegalStateException(s"${service.sdkId} uses SigV4 but has no signing name")
      )
      SigV4.sign(request, config.credentials.resolve(), config.region, signingName, config.clock())
    } else request
}

/** Pagination over `@paginated` operations: one page per call, until there is no next token. */
object Paginator {
  def pages[I, O, T](first: I, call: I => O, nextToken: O => Option[T], withToken: (I, T) => I): Iterator[O] =
    Iterator.unfold(Option(first)) { next =>
      next.map { input =>
        val output = call(input)
        output -> nextToken(output).filter(nonEmpty).map(t => withToken(input, t))
      }
    }

  private def nonEmpty(token: Any): Boolean = token match {
    case s: String          => s.nonEmpty
    case m: collection.Map[?, ?] => m.nonEmpty
    case _                  => true
  }
}
