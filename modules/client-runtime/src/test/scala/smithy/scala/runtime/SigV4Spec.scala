package smithy.scala.runtime

import java.net.URI
import java.time.Instant

/** Cases from the AWS SigV4 test suite (get-vanilla, post-vanilla, get-vanilla-query-order-key). */
class SigV4Spec extends munit.FunSuite {
  private val creds = Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")
  private val now   = Instant.parse("2015-08-30T12:36:00Z")

  private def sign(method: String, uri: String): String =
    SigV4
      .sign(HttpRequest(method, URI.create(uri), Headers.empty, Array.emptyByteArray), creds, "us-east-1", "service", now)
      .headers
      .get("Authorization")
      .getOrElse(fail("no Authorization header"))

  private val prefix =
    "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=host;x-amz-date, Signature="

  test("get-vanilla") {
    assertEquals(sign("GET", "https://example.amazonaws.com/"), prefix + "5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31")
  }

  test("post-vanilla") {
    assertEquals(sign("POST", "https://example.amazonaws.com/"), prefix + "5da7c1a2acd57cee7505fc6676e4e544621c30862966e37dddb68e92efbe5d6b")
  }

  test("get-vanilla-query-order-key") {
    assertEquals(
      sign("GET", "https://example.amazonaws.com/?Param2=value2&Param1=value1"),
      prefix + "b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500"
    )
  }

  test("session token is added and signed") {
    val signed = SigV4.sign(
      HttpRequest("POST", URI.create("https://sqs.us-east-1.amazonaws.com/"), Headers.empty, Array.emptyByteArray),
      creds.copy(sessionToken = Some("token")),
      "us-east-1",
      "sqs",
      now
    )
    assertEquals(signed.headers.get("X-Amz-Security-Token"), Some("token"))
    assert(signed.headers.get("Authorization").exists(_.contains("SignedHeaders=host;x-amz-date;x-amz-security-token")))
  }
}
