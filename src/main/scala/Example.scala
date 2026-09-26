package example

import org.apache.fory.json.{ForyJson, JsonTypeChecker}
import org.apache.fory.json.annotation.JsonProperty
import org.apache.fory.json.scala.ForyJsonScala
import org.apache.fory.json.PropertyNamingStrategy
import org.apache.fory.scala.ForyScala

object Example {

  case class Person(name: String, age: Int)

  val fory = ForyScala.builder().withXlang(true).build()
  fory.register(classOf[Person], "example.Person")

  val bytes  = fory.serialize(Person("Alice", 30))
  val person = fory.deserialize(bytes).asInstanceOf[Person]
  println(person.name)

  object AppJson {

    val instance: ForyJson =
      ForyJsonScala
        .builder()
        .withConcurrencyLevel(256)
        // --- untrusted-input hardening ---
        .withTypeChecker(typeChecker)
        .maxDepth(24)
        .withMaxGraphMemoryBytes(16L * 1024 * 1024)
        // --- wire shape ---
        .defaultPropertyInclusion(JsonProperty.Include.NON_NULL)
        .withPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .build()

    private val typeChecker: JsonTypeChecker =
      (className, _) => className.startsWith("com.example.model.")

  }

}
