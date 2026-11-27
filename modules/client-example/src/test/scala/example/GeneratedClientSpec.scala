package example

import java.time.Instant

import smithy.scala.runtime.*

import example.dynamodb.AttributeValue
import example.dynamodb.DynamoDBClient
import example.dynamodb.GetItemInput
import example.dynamodb.Put
import example.dynamodb.PutItemInput
import example.dynamodb.TransactWriteItem
import example.dynamodb.TransactWriteItemsInput
import example.sqs.ListQueuesRequest
import example.sqs.QueueDoesNotExist
import example.sqs.SQSClient
import example.sqs.SendMessageRequest

import FakeAwsServer.json

/** Generated clients against a local server: what goes on the wire, and what comes back. */
class GeneratedClientSpec extends munit.FunSuite {

  private val server = FunFixture[FakeAwsServer](_ => new FakeAwsServer, _.close())

  private def config(s: FakeAwsServer) = ClientConfig(
    region = "us-east-1",
    endpoint = Some(s.endpoint),
    credentials = CredentialsProvider.static(Credentials("AKIDEXAMPLE", "secret")),
    clock = () => Instant.parse("2026-11-26T12:00:00Z"),
    sleep = _ => ()
  )

  server.test("SQS SendMessage: awsJson1_0 request, signed, and the output parsed") { s =>
    s.reply(200, """{"MessageId":"m-1","MD5OfMessageBody":"49f68a5c8493ec2c0bf489821c21fc3b"}""")
    val out = SQSClient(config(s)).sendMessage(SendMessageRequest("https://sqs.local/1/q", "hi", delaySeconds = Some(5)))

    assertEquals(out.messageId, Some("m-1"))
    assertEquals(out.md5OfMessageBody, Some("49f68a5c8493ec2c0bf489821c21fc3b"))

    val req = s.requests.head
    assertEquals(req.method, "POST")
    assertEquals(req.path, "/")
    assertEquals(req.headers.get("x-amz-target"), Some("AmazonSQS.SendMessage"))
    assertEquals(req.headers.get("content-type"), Some("application/x-amz-json-1.0"))
    assertEquals(req.headers.get("x-amz-date"), Some("20261126T120000Z"))
    assert(
      req.headers.get("authorization").exists(_.startsWith(
        "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20261126/us-east-1/sqs/aws4_request, " +
          "SignedHeaders=content-type;host;x-amz-date;x-amz-target, Signature="
      )),
      req.headers.get("authorization")
    )
    assertEquals(req.json, json("""{"QueueUrl":"https://sqs.local/1/q","MessageBody":"hi","DelaySeconds":5}"""))
  }

  server.test("a modelled error becomes its generated class, with metadata") { s =>
    s.reply(
      400,
      """{"__type":"com.amazonaws.sqs#QueueDoesNotExist","message":"The queue does not exist"}""",
      "x-amzn-RequestId" -> "req-123"
    )
    val e = intercept[QueueDoesNotExist](SQSClient(config(s)).sendMessage(SendMessageRequest("q", "hi")))
    assertEquals(e.message, Some("The queue does not exist"))
    assertEquals(e.getMessage, "The queue does not exist")
    assertEquals(e.smithyMetadata.map(_.statusCode), Some(400))
    assertEquals(e.smithyMetadata.flatMap(_.requestId), Some("req-123"))
    assertEquals(s.requests.size, 1) // client errors aren't retried
  }

  server.test("an unknown error code becomes UnknownServiceException") { s =>
    s.reply(400, """{"__type":"SomethingNew","message":"?"}""")
    val e = intercept[UnknownServiceException](SQSClient(config(s)).sendMessage(SendMessageRequest("q", "hi")))
    assertEquals(e.code, "SomethingNew")
  }

  server.test("5xx and throttling are retried; the body is serialized once and re-signed") { s =>
    s.reply(503, """{"__type":"ServiceUnavailable"}""")
    s.reply(400, """{"__type":"com.amazonaws.sqs#RequestThrottled","message":"slow down"}""")
    s.reply(200, """{"MessageId":"m-3"}""")
    val out = SQSClient(config(s)).sendMessage(SendMessageRequest("q", "hi"))
    assertEquals(out.messageId, Some("m-3"))
    assertEquals(s.requests.size, 3)
    assertEquals(s.requests.map(_.body).distinct.size, 1)
  }

  server.test("retries stop at maxAttempts") { s =>
    (1 to 3).foreach(_ => s.reply(500, """{"__type":"InternalError"}"""))
    intercept[UnknownServiceException](SQSClient(config(s)).sendMessage(SendMessageRequest("q", "hi"))): Unit
    assertEquals(s.requests.size, 3)
  }

  server.test("paginators follow NextToken") { s =>
    s.reply(200, """{"QueueUrls":["q1","q2"],"NextToken":"t1"}""")
    s.reply(200, """{"QueueUrls":["q3"]}""")
    val pages = SQSClient(config(s)).listQueuesPaginator(ListQueuesRequest(maxResults = Some(2))).toList
    assertEquals(pages.flatMap(_.queueUrls.getOrElse(Nil)), List("q1", "q2", "q3"))
    assertEquals(s.requests.map(_.json), List(json("""{"MaxResults":2}"""), json("""{"NextToken":"t1","MaxResults":2}""")))
  }

  server.test("DynamoDB: a recursive union on the wire and back") { s =>
    s.reply(200, """{"Item":{"pk":{"S":"a"},"nested":{"M":{"n":{"N":"1"},"l":{"L":[{"BOOL":true},{"NULL":true}]}}},"x":{"NEW":1}}}""")
    val client = DynamoDBClient(config(s))

    val item = Map(
      "pk"     -> AttributeValue.S("a"),
      "nested" -> AttributeValue.M(Map("n" -> AttributeValue.N("1")))
    )
    client.putItem(PutItemInput("t", item)): Unit
    assertEquals(s.requests.head.headers.get("x-amz-target"), Some("DynamoDB_20120810.PutItem"))
    assertEquals(
      s.requests.head.json,
      json("""{"TableName":"t","Item":{"pk":{"S":"a"},"nested":{"M":{"n":{"N":"1"}}}}}""")
    )

    s.reply(200, """{"Item":{"pk":{"S":"a"},"nested":{"M":{"n":{"N":"1"},"l":{"L":[{"BOOL":true},{"NULL":true}]}}},"x":{"NEW":1}}}""")
    val got = client.getItem(GetItemInput("t", Map("pk" -> AttributeValue.S("a")))).item.getOrElse(Map.empty)
    assertEquals(got.get("pk"), Some(AttributeValue.S("a")))
    assertEquals(
      got.get("nested"),
      Some(AttributeValue.M(Map("n" -> AttributeValue.N("1"), "l" -> AttributeValue.L(List(AttributeValue.BOOL(true), AttributeValue.NULL(true))))))
    )
    assertEquals(got.get("x"), Some(AttributeValue.UnknownMember("NEW")))
  }

  server.test("@idempotencyToken members are filled in when left empty") { s =>
    s.reply(200, "{}")
    DynamoDBClient(config(s)).transactWriteItems(
      TransactWriteItemsInput(List(TransactWriteItem(put = Some(Put(Map("pk" -> AttributeValue.S("a")), "t")))))
    ): Unit
    val token = s.requests.head.json.path("ClientRequestToken").asString("")
    assert(token.matches("[0-9a-f-]{36}"), token)
  }
}

/** The rpcv2Json client generated from model/weather.smithy. */
class RpcV2JsonClientSpec extends munit.FunSuite {
  import example.weather.*

  private val server = FunFixture[FakeAwsServer](_ => new FakeAwsServer, _.close())

  private def client(s: FakeAwsServer) =
    WeatherClient(ClientConfig(endpoint = Some(s.endpoint), sleep = _ => ()))

  server.test("request path, headers and body; defaults are sent; no auth") { s =>
    s.reply(200, """{"days":[{"date":1795687200,"copy":"c"},null],"type":{"sunny":{}},"next":null}""")
    val out = client(s).getForecast(GetForecastInput("Oslo"))

    val req = s.requests.head
    assertEquals(req.path, "/service/Weather/operation/GetForecast")
    assertEquals(req.headers.get("smithy-protocol"), Some("rpc-v2-json"))
    assertEquals(req.headers.get("content-type"), Some("application/json"))
    assertEquals(req.headers.get("authorization"), None)
    assertEquals(req.json, FakeAwsServer.json("""{"city":"Oslo","units":"METRIC","detail":1}"""))

    // rpcv2Json ignores @timestampFormat: timestamps are always epoch seconds.
    assertEquals(out.days, Some(List(Some(Day(Some(Instant.parse("2026-11-26T10:00:00Z")), Some("c"))), None)))
    assertEquals(out.`type`, Kind.Sunny)
    assertEquals(out.next, None)
  }

  server.test("Unit input sends no body") { s =>
    s.reply(200, "")
    client(s).ping()
    assertEquals(s.requests.head.body, "")
    assertEquals(s.requests.head.headers.get("content-type"), None)
  }

  server.test("a @retryable(throttling) error is retried, then thrown") { s =>
    (1 to 3).foreach(_ => s.reply(500, """{"__type":"example.weather#Busy","message":"later"}"""))
    val e = intercept[Busy](client(s).getForecast(GetForecastInput("Oslo")))
    assertEquals(e.message, Some("later"))
    assert(e.smithyThrottling)
    assertEquals(s.requests.size, 3)
  }
}
