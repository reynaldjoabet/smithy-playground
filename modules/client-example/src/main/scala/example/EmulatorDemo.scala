package example

import java.io.FileInputStream
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

import smithy.scala.runtime.*

import example.cognitoidp.*
import example.dynamodb.*
import example.secretsmanager.*
import example.sqs.*

/**
  * Runs the generated clients against a local AWS Emulator and prints every HTTP exchange with all
  * its headers. The access key id and signatures are redacted.
  *
  * Run with: sbt --client "clientExample/runMain example.EmulatorDemo [sqs] [dynamodb] [secretsmanager] [cognito]"
  * With no service named it runs SQS and DynamoDB. (KMS has no emulator to run against.)
  */
object EmulatorDemo {
  private val emulatorDir: Path = Paths.get(sys.props("user.home"), ".aws", "emulator", "9297")

  def main(args: Array[String]): Unit = {
    // --jdk-log also prints java.net.http's own header log, to compare with the one below.
    // It isn't redacted, so it shows the real access key id and signatures.
    if (args.contains("--jdk-log")) System.setProperty("jdk.httpclient.HttpClient.log", "headers"): Unit
    val endpoint = URI.create(sys.env.getOrElse("AWS_ENDPOINT_URL", "https://127.0.0.1:9297"))
    val config = ClientConfig(
      region = "us-east-1",
      endpoint = Some(endpoint),
      credentials = CredentialsProvider.static(emulatorCredentials()),
      transport = JavaHttpTransport(sslContext = Some(trusting(emulatorDir.resolve("ca.pem")))),
      interceptors = List(WireLog)
    )
    val named = args.filterNot(_.startsWith("--")).toSet
    val run   = if (named.isEmpty) Set("sqs", "dynamodb") else named
    if (run("sqs")) sqs(SQSClient(config))
    if (run("dynamodb")) dynamodb(DynamoDBClient(config))
    if (run("secretsmanager")) secretsManager(SecretsManagerClient(config))
    if (run("cognito")) cognito(CognitoIdentityProviderClient(config))
  }

  // -------------------------------------------------------------------------------------------- Secrets Manager

  private def secretsManager(client: SecretsManagerClient): Unit = {
    // A fresh name each run: the emulator keeps a deleted secret "marked for deletion", so a fixed name would clash.
    val name = s"scala-client-demo/db-password-${System.currentTimeMillis}"

    section("Secrets Manager: CreateSecret (ClientRequestToken filled in by @idempotencyToken)")
    val created = client.createSecret(CreateSecretRequest(name, description = Some("demo"), secretString = Some("s3cr3t")))
    println(s"-> arn = ${created.arn.getOrElse("?")}, versionId = ${created.versionId.getOrElse("?")}")

    section("Secrets Manager: GetSecretValue")
    val value = client.getSecretValue(GetSecretValueRequest(name))
    println(s"-> secretString = ${value.secretString.getOrElse("?")}, createdDate = ${value.createdDate.getOrElse("?")}")

    section("Secrets Manager: ListSecrets (paginator)")
    val names = client.listSecretsPaginator(ListSecretsRequest()).flatMap(_.secretList.getOrElse(Nil)).flatMap(_.name).toList
    println(s"-> found = ${names.contains(name)} (${names.size} secrets listed)")

    section("Secrets Manager: DeleteSecret")
    client.deleteSecret(DeleteSecretRequest(name, forceDeleteWithoutRecovery = Some(true))): Unit

    section("Secrets Manager: GetSecretValue on the deleted secret (modelled error)")
    // AWS answers ResourceNotFoundException once a forced delete completes; the emulator answers
    // InvalidRequestException ("marked for deletion"). Both are modelled errors of GetSecretValue.
    try client.getSecretValue(GetSecretValueRequest(name)): Unit
    catch {
      case e: (example.secretsmanager.ResourceNotFoundException | example.secretsmanager.InvalidRequestException) =>
        println(s"-> threw ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
  }

  // -------------------------------------------------------------------------------------------- Cognito

  private def cognito(client: CognitoIdentityProviderClient): Unit = {
    section("Cognito: CreateUserPool")
    val poolId = client.createUserPool(CreateUserPoolRequest("scala-client-demo")).userPool.flatMap(_.id).getOrElse(sys.error("no pool id"))
    println(s"-> userPool.id = $poolId")

    section("Cognito: AdminCreateUser")
    val user = client.adminCreateUser(
      AdminCreateUserRequest(
        poolId,
        "ada",
        userAttributes = Some(List(AttributeType("email", Some("ada@example.com")))),
        messageAction = Some(MessageActionType.SUPPRESS)
      )
    )
    println(s"-> user = ${user.user.flatMap(_.username).getOrElse("?")}, status = ${user.user.flatMap(_.userStatus).getOrElse("?")}")

    section("Cognito: AdminCreateUser again (modelled error)")
    try client.adminCreateUser(AdminCreateUserRequest(poolId, "ada", messageAction = Some(MessageActionType.SUPPRESS))): Unit
    catch { case e: UsernameExistsException => println(s"-> threw ${e.getClass.getSimpleName}: ${e.getMessage}") }

    section("Cognito: ListUserPools (paginator)")
    val pools = client.listUserPoolsPaginator(ListUserPoolsRequest(maxResults = 10)).flatMap(_.userPools.getOrElse(Nil)).flatMap(_.name).toList
    println(s"-> pools = $pools")

    section("Cognito: DeleteUserPool")
    client.deleteUserPool(DeleteUserPoolRequest(poolId))

    section("Cognito: DescribeUserPool on the deleted pool (modelled error)")
    try client.describeUserPool(DescribeUserPoolRequest(poolId)): Unit
    catch { case e: example.cognitoidp.ResourceNotFoundException => println(s"-> threw ${e.getClass.getSimpleName}: ${e.getMessage}") }
  }

  // -------------------------------------------------------------------------------------------- SQS

  private def sqs(client: SQSClient): Unit = {
    section("SQS: CreateQueue")
    val url = client.createQueue(CreateQueueRequest("scala-client-demo")).queueUrl.getOrElse(sys.error("no QueueUrl"))

    section("SQS: SendMessage")
    client.sendMessage(SendMessageRequest(url, "hello from Scala", delaySeconds = Some(0))): Unit

    section("SQS: ReceiveMessage")
    val received = client.receiveMessage(ReceiveMessageRequest(url, maxNumberOfMessages = Some(1), waitTimeSeconds = Some(1)))
    val handle   = received.messages.getOrElse(Nil).headOption.flatMap(_.receiptHandle)

    section("SQS: DeleteMessage")
    handle.foreach(h => client.deleteMessage(DeleteMessageRequest(url, h)))

    section("SQS: ListQueues (paginator)")
    client.listQueuesPaginator(ListQueuesRequest(queueNamePrefix = Some("scala-client"))).foreach(_ => ())

    section("SQS: DeleteQueue")
    client.deleteQueue(DeleteQueueRequest(url))

    section("SQS: SendMessage to a deleted queue (modelled error)")
    try client.sendMessage(SendMessageRequest(url, "too late")): Unit
    catch { case e: QueueDoesNotExist => println(s"-> threw ${e.getClass.getSimpleName}: ${e.getMessage}") }
  }

  // -------------------------------------------------------------------------------------------- DynamoDB

  private def dynamodb(client: DynamoDBClient): Unit = {
    val table = "scala-client-demo"

    // A table left by an interrupted run is deleted first, without logging.
    WireLog.quietly {
      try {
        client.deleteTable(DeleteTableInput(table)): Unit
        waitFor {
          try { client.describeTable(DescribeTableInput(table)): Unit; false }
          catch { case _: example.dynamodb.ResourceNotFoundException => true }
        }: Unit
      } catch { case _: example.dynamodb.ResourceNotFoundException => () }
    }

    section("DynamoDB: CreateTable")
    client.createTable(
      CreateTableInput(
        attributeDefinitions = Some(List(AttributeDefinition("pk", ScalarAttributeType.S))),
        tableName = table,
        keySchema = Some(List(KeySchemaElement("pk", KeyType.HASH))),
        billingMode = Some(BillingMode.PAY_PER_REQUEST)
      )
    ): Unit
    val polls = WireLog.quietly(waitFor(client.describeTable(DescribeTableInput(table)).table.flatMap(_.tableStatus).contains(TableStatus.ACTIVE)))
    println(s"-> (waited for ACTIVE: $polls DescribeTable calls, not shown)")

    section("DynamoDB: PutItem (nested map and list)")
    client.putItem(
      PutItemInput(
        table,
        Map(
          "pk"   -> AttributeValue.S("item-1"),
          "tags" -> AttributeValue.L(List(AttributeValue.S("red"), AttributeValue.N("42"))),
          "meta" -> AttributeValue.M(Map("ok" -> AttributeValue.BOOL(true)))
        )
      )
    ): Unit

    section("DynamoDB: GetItem")
    val item = client.getItem(GetItemInput(table, Map("pk" -> AttributeValue.S("item-1")))).item
    println(s"-> decoded: $item")

    section("DynamoDB: DeleteTable")
    client.deleteTable(DeleteTableInput(table)): Unit
    val gone = WireLog.quietly(waitFor {
      try { client.describeTable(DescribeTableInput(table)): Unit; false }
      catch { case _: example.dynamodb.ResourceNotFoundException => true }
    })
    println(s"-> (waited for the table to go: $gone DescribeTable calls, not shown)")

    section("DynamoDB: GetItem on the deleted table (modelled error)")
    try client.getItem(GetItemInput(table, Map("pk" -> AttributeValue.S("item-1")))): Unit
    catch { case e: example.dynamodb.ResourceNotFoundException => println(s"-> threw ${e.getClass.getSimpleName}: ${e.getMessage}") }
  }

  // -------------------------------------------------------------------------------------------- logging

  private def section(title: String): Unit = println(s"\n=== $title ===")

  /** Polls until `done`, up to 50 times 100 ms apart; returns how many calls it took. */
  private def waitFor(done: => Boolean): Int = {
    var n = 1
    while (!done && n < 50) { Thread.sleep(100); n += 1 }
    n
  }

  /**
    * Prints each attempt as raw HTTP. The request shows every header on the wire: the
    * `Content-Length` and `Host` that java.net.http adds (from [[JavaHttpTransport.addedHeaders]]),
    * then the client's own, including `User-Agent`. The access key id and signature are redacted.
    */
  private object WireLog extends Interceptor {
    @volatile private var quiet = false

    def quietly[A](body: => A): A = {
      quiet = true
      try body
      finally quiet = false
    }

    override def afterAttempt(request: HttpRequest, result: Either[Throwable, HttpResponse]): Unit =
      if (!quiet) print(request, result)

    private def print(request: HttpRequest, result: Either[Throwable, HttpResponse]): Unit = {
      val path     = Option(request.uri.getRawPath).filter(_.nonEmpty).getOrElse("/")
      val protocol = result.map(_.protocol).getOrElse("HTTP/1.1")
      val onWire   = if (protocol == "HTTP/1.1") JavaHttpTransport.addedHeaders(request).entries else Vector.empty
      println(s"${request.method} $path $protocol")
      (onWire ++ request.headers.entries).foreach(h => println(s"${h.name}: ${redact(h.name, h.value)}"))
      println()
      println(new String(request.body, UTF_8))
      println()
      result match {
        case Right(response) =>
          println(s"${response.protocol} ${response.status}")
          response.headers.entries.foreach(h => println(s"${h.name}: ${h.value}"))
          println()
          println(new String(response.body, UTF_8))
        case Left(e) => println(s"(no response: $e)")
      }
    }

    private def redact(name: String, value: String): String =
      if (name.equalsIgnoreCase("Authorization"))
        value.replaceAll("Credential=[^/]+/", "Credential=<access-key-id>/").replaceAll("Signature=[0-9a-f]+", "Signature=<signature>")
      else if (name.equalsIgnoreCase("X-Amz-Security-Token")) "<redacted>"
      else value
  }

  // -------------------------------------------------------------------------------------------- setup

  /** The `[default]` keys from the credentials file the emulator writes. */
  private def emulatorCredentials(): Credentials = {
    val kv = Files
      .readAllLines(emulatorDir.resolve("emulator_credentials"), UTF_8)
      .toArray(Array.empty[String])
      .toList
      .flatMap { line =>
        line.split("=", 2) match {
          case Array(k, v) => List(k.trim -> v.trim)
          case _           => Nil
        }
      }
      .toMap
    Credentials(kv("aws_access_key_id"), kv("aws_secret_access_key"), kv.get("aws_session_token"))
  }

  /** An SSLContext that trusts the emulator's self-signed certificate authority. */
  private def trusting(caPem: Path): SSLContext = {
    val cert  = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(caPem.toFile))
    val store = KeyStore.getInstance(KeyStore.getDefaultType)
    store.load(null, null)
    store.setCertificateEntry("aws-emulator", cert)
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(store)
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, tmf.getTrustManagers, null)
    ctx
  }
}
