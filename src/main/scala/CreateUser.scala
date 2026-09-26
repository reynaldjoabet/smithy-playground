package example

import scala.collection.immutable.NumericRange

import org.apache.fory.json.annotation.JsonIgnore
import org.apache.fory.json.annotation.JsonProperty
import org.apache.fory.json.scala.ScalaTypeRef
import org.apache.fory.json.ForyJson
import org.apache.fory.json.JsonTypeChecker
import org.apache.fory.json.PropertyNamingStrategy
import org.apache.fory.reflect.TypeRef

final case class CreateUser(
    @JsonProperty("user_name") userName: String, // required: absent => error
    email: Option[String] = None,                // absent => None
    roles: List[String] = Nil,                   // absent => Nil
    @JsonIgnore auditToken: String = ""          // never on the wire
)

val json: ForyJson =
  ForyJsonScala
    .builder()
    .withConcurrencyLevel(256)
    // --- untrusted-input hardening ---
    // .withTypeChecker(typeChecker)
    .maxDepth(24)
    .withMaxGraphMemoryBytes(16L * 1024 * 1024)
    // --- wire shape ---
    .defaultPropertyInclusion(JsonProperty.Include.NON_NULL)
    .withPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    .build()

// // Normal generic types
// val page = json.fromJson(body, new TypeRef[Page[CreateUser]]() {})

// When a Scala value type would erase to Object
val range = json.fromJson("[1,3,5]", ScalaTypeRef[NumericRange[Int]])

import org.apache.fory.json.annotation.JsonSubTypes
import org.apache.fory.json.scala.*

@JsonSubTypes(property = "kind")
sealed trait Event derives ScalaJsonCodec

final case class Message(value: String) extends Event
case object Idle                        extends Event

enum Result derives ScalaJsonCodec {

  case Ok(value: String)
  case Error(code: Int)
  case Pending // => {"Pending":{}}

}

import org.apache.fory.json.scala.ForyJsonScala

case class Person(name: String, age: Int = 18, aliases: List[String] = Nil)

val json2  = ForyJsonScala.builder().build()
val text   = json.toJson(Person("Ada"))
val person = json.fromJson(text, classOf[Person])
